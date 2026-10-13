package com.knowledge.base.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.base.ai.dto.AiChatRequest;
import com.knowledge.base.ai.dto.ChunkHit;
import com.knowledge.base.ai.event.ArticleContentChangedEvent;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.service.RedisLuaService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 知识库问答：检索 → 组装提示词 → 流式生成。
 * <p>
 * SSE 事件协议：每条 data 是一个 JSON 对象，
 * {"type":"status","message":"..."} 阶段提示 / {"type":"articles","data":[...]} 检索候选文章（前端作推荐卡片对照表）/
 * {"type":"token","content":"..."} 流式片段 / {"type":"error","message":"..."} 出错 /
 * {"type":"done"} 结束（正常结束时直接 complete，done 事件作为冗余保险）。
 */
@Slf4j
@Service
public class KnowledgeBaseService {

    /** 检索返回的分块数 */
    private static final int TOP_K = 5;

    /** 前端历史最多取最近几轮 */
    private static final int MAX_HISTORY = 6;

    /** 历史消息单条最长保留字符数，控制 token 消耗 */
    private static final int MAX_HISTORY_CONTENT = 500;

    @Autowired
    private StreamingChatModel streamingChatModel;

    @Autowired
    private ChunkVectorStore chunkVectorStore;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisLuaService redisLuaService;

    @Value("${ai.chat.rate-limit-per-minute:20}")
    private int rateLimitPerMinute;

    /** 重建全量索引（异步执行，接口立即返回） */
    @Async("aiExecutor")
    public void rebuildAllAsync() {
        chunkVectorStore.rebuildAll();
    }

    /** 文章发布/编辑/删除后（事务提交），异步重建该文章的向量索引 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("aiExecutor")
    public void onArticleContentChanged(ArticleContentChangedEvent event) {
        try {
            chunkVectorStore.rebuildArticle(event.getArticleId());
        } catch (Exception e) {
            log.error("文章 {} 向量索引重建失败", event.getArticleId(), e);
        }
    }

    /**
     * RAG 流式问答：限流 → 检索 → 组装提示词 → 流式推送 SSE。
     * 异步执行（aiChatExecutor）：控制器立即返回 emitter 建立 SSE 连接，
     * 各阶段通过 status 事件向前端反馈进度，任何异常都以 error 事件快速返回，
     * 不会出现"连接建立后无声无息"的等待。
     */
    @Async("aiChatExecutor")
    public void chatAsync(Long userId, AiChatRequest request, SseEmitter emitter) {
        log.info("AI 问答开始 userId={} question={}", userId, request.getQuestion());
        try {
            checkRateLimit(userId);
            sendEvent(emitter, Map.of("type", "status", "message", "正在检索知识库…"));
            List<ChunkHit> hits = chunkVectorStore.search(request.getQuestion(), TOP_K);
            if (hits.isEmpty()) {
                sendEvent(emitter, Map.of("type", "token",
                        "content", "知识库中暂未收录相关资料，去首页看看其他文章吧。"));
                emitter.complete();
                return;
            }

            // 把检索命中的候选文章推给前端（按相似度降序，同一文章去重）。
            // 前端不直接展示，而是等 AI 在回答末尾输出推荐 JSON 后，按 articleId 对照出要展示的推荐卡片
            List<Map<String, Object>> articleCards = hits.stream()
                    .collect(Collectors.toMap(ChunkHit::getArticleId, h -> h, (a, b) -> a, LinkedHashMap::new))
                    .values().stream()
                    .map(h -> {
                        Map<String, Object> card = new LinkedHashMap<>();
                        card.put("articleId", h.getArticleId());
                        card.put("title", h.getArticleTitle());
                        card.put("summary", h.getArticleSummary() == null ? "" : h.getArticleSummary());
                        return card;
                    })
                    .toList();
            sendEvent(emitter, Map.of("type", "articles", "data", articleCards));
            sendEvent(emitter, Map.of("type", "status", "message", "正在生成回答…"));

            String context = hits.stream()
                    .map(h -> {
                        String summary = h.getArticleSummary() == null || h.getArticleSummary().isEmpty()
                                ? "无" : h.getArticleSummary();
                        return "【文章ID：" + h.getArticleId() + "】《" + h.getArticleTitle() + "》\n"
                                + "摘要：" + summary + "\n内容：\n" + h.getContent();
                    })
                    .collect(Collectors.joining("\n\n"));
            SystemMessage system = SystemMessage.from("""
                    你是知识库的 AI 助手。下面是知识库检索到的资料（每篇标注了文章ID、标题、摘要和内容），供你参考回答用户问题。

                    严格要求：
                    1. 必须用自己的话组织答案，严禁照抄、复制或大段引用资料的原文；
                    2. 资料不足时，明确回答"知识库中没有找到相关资料"，不要编造；
                    3. 回答使用 Markdown 格式；
                    4. 当用户的问题适合推荐具体文章时（如"推荐几篇""有哪些文章"）：
                       - 对每篇推荐文章，只用 1-2 句话介绍（标题 + 内容要点），不得输出文章原文；
                       - 在回答的最后一行严格输出以下 JSON 格式的推荐列表（articleId 必须取自资料中的【文章ID】，最多 3 篇；不推荐时输出空数组）：
                    ```json
                    {"recommend":[{"articleId":1},{"articleId":2}]}
                    ```

                    资料：
                    %s
                    """.formatted(context));

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(system);
            messages.addAll(buildHistory(request));
            messages.add(UserMessage.from(request.getQuestion()));

            streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    sendEvent(emitter, Map.of("type", "token", "content", partialResponse));
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    log.info("AI 问答完成 userId={}", userId);
                    sendEvent(emitter, Map.of("type", "done"));
                    emitter.complete();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("AI 流式问答失败 userId={}", userId, error);
                    sendEvent(emitter, Map.of("type", "error", "message", "AI 服务调用失败，请稍后重试"));
                    emitter.complete();
                }
            });
        } catch (BusinessException e) {
            // 限流、索引未建立等业务错误：转成 SSE 错误事件，前端同样弹提示
            sendEvent(emitter, Map.of("type", "error", "message", e.getMessage()));
            emitter.complete();
        } catch (Exception e) {
            log.error("AI 问答异常 userId={}", userId, e);
            sendEvent(emitter, Map.of("type", "error", "message", "AI 服务调用失败，请稍后重试"));
            emitter.complete();
        }
    }

    /** 前端历史转 ChatMessage：role 不是 assistant 的一律按 user 处理，内容截断防超长 */
    private List<ChatMessage> buildHistory(AiChatRequest request) {
        if (request.getHistory() == null || request.getHistory().isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, request.getHistory().size() - MAX_HISTORY);
        List<ChatMessage> messages = new ArrayList<>();
        for (AiChatRequest.HistoryMessage m : request.getHistory().subList(from, request.getHistory().size())) {
            String content = m.getContent() == null ? ""
                    : m.getContent().length() > MAX_HISTORY_CONTENT
                    ? m.getContent().substring(0, MAX_HISTORY_CONTENT) : m.getContent();
            if ("assistant".equalsIgnoreCase(m.getRole())) {
                messages.add(AiMessage.from(content));
            } else {
                messages.add(UserMessage.from(content));
            }
        }
        return messages;
    }

    /**
     * 每用户每分钟提问次数限流（控制 API 费用）。
     * Redis Lua 原子「自增 + 首次设置过期」；Redis 不可用时降级放行，不阻塞用户（沿用本项目缓存降级惯例）。
     */
    private void checkRateLimit(Long userId) {
        Long count = redisLuaService.increment("ai:chat:limit:" + userId, 60, false);
        if (count != null && count > rateLimitPerMinute) {
            throw new BusinessException("提问太频繁，请稍后再试");
        }
    }

    /** 发送 SSE 事件；客户端断开等异常静默忽略 */
    private void sendEvent(SseEmitter emitter, Object payload) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            // 客户端断开连接，忽略
        }
    }
}
