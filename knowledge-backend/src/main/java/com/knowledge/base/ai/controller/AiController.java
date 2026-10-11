package com.knowledge.base.ai.controller;

import com.knowledge.base.ai.dto.AiChatRequest;
import com.knowledge.base.ai.dto.AiGenerateRequest;
import com.knowledge.base.ai.dto.AiSummaryVO;
import com.knowledge.base.ai.service.AiSummaryService;
import com.knowledge.base.ai.service.KnowledgeBaseService;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.common.Result;
import com.knowledge.base.entity.User;
import com.knowledge.base.vo.TagVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 能力接口。
 * /api/ai/** 未在 SecurityConfig 的 permitAll 清单中，自动要求登录。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiSummaryService aiSummaryService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /** AI 生成文章摘要（同步，前端按钮触发） */
    @PostMapping("/summary")
    public Result<AiSummaryVO> generateSummary(@Valid @RequestBody AiGenerateRequest request,
                                               @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        return Result.success(aiSummaryService.generateSummary(request.getTitle(), request.getContent()));
    }

    /** AI 从现有标签库中推荐标签（同步，前端按钮触发） */
    @PostMapping("/tags")
    public Result<List<TagVO>> recommendTags(@Valid @RequestBody AiGenerateRequest request,
                                             @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        return Result.success(aiSummaryService.recommendTags(request.getTitle(), request.getContent()));
    }

    /**
     * RAG 流式问答（SSE）。
     * 创建 emitter 前抛出的业务异常由全局异常处理器返回 JSON，
     * 前端根据响应 Content-Type 区分 SSE 流与 JSON 错误。
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request,
                           @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new BusinessException("请先登录");
        }
        SseEmitter emitter = new SseEmitter(180_000L);
        knowledgeBaseService.chat(user.getId(), request, emitter);
        return emitter;
    }

    /** 管理员手动重建全量向量索引（异步执行，立即返回） */
    @PostMapping("/rebuild-index")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Void> rebuildIndex() {
        knowledgeBaseService.rebuildAllAsync();
        return Result.success();
    }
}
