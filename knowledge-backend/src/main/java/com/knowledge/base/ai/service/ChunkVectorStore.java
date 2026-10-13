package com.knowledge.base.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.ai.dto.ChunkHit;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.entity.AiArticleChunk;
import com.knowledge.base.entity.Article;
import com.knowledge.base.mapper.AiArticleChunkMapper;
import com.knowledge.base.mapper.ArticleMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 文章分块向量库：MySQL 持久化 + 内存缓存。
 * <p>
 * 检索时问题向量与全部缓存向量做余弦相似度（向量已归一化，即点积），
 * 暴力遍历取 Top-K。当前数据量（几千篇文章 ≈ 几万块 × 1024 维）毫秒级即可完成；
 * 数据量进一步增长时可平滑演进为 Milvus/pgvector 等专业向量库，接口不变。
 */
@Slf4j
@Component
public class ChunkVectorStore {

    /** 每块约 500 字 */
    private static final int CHUNK_SIZE = 500;

    /** 块间重叠 50 字，减少切块截断带来的语义丢失 */
    private static final int CHUNK_OVERLAP = 50;

    /** chunkId -> 归一化向量。只缓存向量，正文仍在 MySQL，命中后按 id 回查 */
    private final Map<Long, float[]> vectorCache = new ConcurrentHashMap<>();

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private AiArticleChunkMapper chunkMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    @Qualifier("aiExecutor")
    private Executor aiExecutor;

    /** 应用启动时把已建索引加载进内存，重启无需重建 */
    @PostConstruct
    public void loadFromDb() {
        try {
            List<AiArticleChunk> chunks = chunkMapper.selectList(null);
            for (AiArticleChunk c : chunks) {
                vectorCache.put(c.getId(), toFloats(c.getVector()));
            }
            log.info("AI 向量索引加载完成，共 {} 条分块", chunks.size());
        } catch (Exception e) {
            // 索引表不存在（未执行迁移）等情况：不阻塞应用启动，稍后通过重建索引接口恢复
            log.error("AI 向量索引加载失败，可稍后通过重建索引接口恢复", e);
        }

        // 自愈：索引为空但存在已发布文章（老库升级首次启用 AI 功能）时，异步全量重建，
        // 避免用户提问时看到“索引尚未建立，请联系管理员”却没有可操作入口
        if (vectorCache.isEmpty()) {
            Long published = articleMapper.selectCount(
                    new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1));
            if (published != null && published > 0) {
                log.info("向量索引为空且存在 {} 篇已发布文章，启动自动全量重建", published);
                aiExecutor.execute(() -> {
                    try {
                        rebuildAll();
                    } catch (Exception e) {
                        log.error("启动自动重建向量索引失败", e);
                    }
                });
            }
        }
    }

    /** 重建单篇文章的索引（删除文章时传 id 即可清空该文索引） */
    public void rebuildArticle(Long articleId) {
        // 先清旧索引（含缓存），再重建
        List<AiArticleChunk> oldChunks = chunkMapper.selectList(
                new LambdaQueryWrapper<AiArticleChunk>().eq(AiArticleChunk::getArticleId, articleId));
        chunkMapper.delete(new LambdaQueryWrapper<AiArticleChunk>().eq(AiArticleChunk::getArticleId, articleId));
        oldChunks.forEach(c -> vectorCache.remove(c.getId()));

        Article article = articleMapper.selectById(articleId);
        // 已删除或未发布的文章不进索引
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            return;
        }
        List<String> chunks = split(article.getContent());
        for (int i = 0; i < chunks.size(); i++) {
            float[] vector = embedAndNormalize(chunks.get(i));
            AiArticleChunk entity = new AiArticleChunk();
            entity.setArticleId(articleId);
            entity.setChunkIndex(i);
            entity.setContent(chunks.get(i));
            entity.setVector(toBytes(vector));
            chunkMapper.insert(entity);
            vectorCache.put(entity.getId(), vector);
        }
    }

    /** 全量重建索引（管理员手动触发，耗时取决于文章量） */
    public void rebuildAll() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1));
        chunkMapper.clearAll();
        vectorCache.clear();
        int failed = 0;
        for (int i = 0; i < articles.size(); i++) {
            try {
                rebuildArticle(articles.get(i).getId());
            } catch (Exception e) {
                // 单篇失败不拖垮全量重建（如 embedding 限流、个别内容异常），记录后继续
                failed++;
                log.error("文章 {} 索引重建失败，已跳过（第 {}/{} 篇）", articles.get(i).getId(), i + 1, articles.size(), e);
            }
            if ((i + 1) % 50 == 0) {
                log.info("AI 索引重建进度：{}/{}", i + 1, articles.size());
            }
            // 篇间轻微间隔，避免短时间密集调用 embedding 触发提供方限流
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("AI 索引重建完成，共 {} 篇文章，成功 {} 篇，失败 {} 篇", articles.size(), articles.size() - failed, failed);
    }

    /**
     * 向量检索：返回与问题最相似的 topK 个分块。
     * 索引为空时抛业务异常，提示管理员先重建索引。
     */
    public List<ChunkHit> search(String question, int topK) {
        if (vectorCache.isEmpty()) {
            throw new BusinessException("知识库索引尚未建立，请联系管理员重建索引");
        }
        float[] questionVector = embedAndNormalize(question);

        // 小顶堆保留相似度最高的 topK 个分块
        PriorityQueue<Map.Entry<Long, Float>> heap = new PriorityQueue<>(
                Comparator.comparingDouble(Map.Entry::getValue));
        for (Map.Entry<Long, float[]> e : vectorCache.entrySet()) {
            float score = dot(questionVector, e.getValue());
            if (heap.size() < topK) {
                heap.offer(new AbstractMap.SimpleEntry<>(e.getKey(), score));
            } else if (score > heap.peek().getValue()) {
                heap.poll();
                heap.offer(new AbstractMap.SimpleEntry<>(e.getKey(), score));
            }
        }

        // 堆内顺序与分数无关，按相似度降序排列，保证推荐卡片的顺序稳定
        List<Long> ids = heap.stream()
                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        List<AiArticleChunk> chunks = chunkMapper.selectBatchIds(ids);
        Set<Long> articleIds = chunks.stream().map(AiArticleChunk::getArticleId).collect(Collectors.toSet());
        Map<Long, String> titleMap = articleMapper.selectBatchIds(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, Article::getTitle, (a, b) -> a));
        Map<Long, String> summaryMap = articleMapper.selectBatchIds(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, Article::getSummary, (a, b) -> a));

        List<ChunkHit> hits = new ArrayList<>();
        for (AiArticleChunk c : chunks) {
            ChunkHit hit = new ChunkHit();
            hit.setArticleId(c.getArticleId());
            hit.setArticleTitle(titleMap.getOrDefault(c.getArticleId(), "未知文章"));
            String summary = summaryMap.get(c.getArticleId());
            hit.setArticleSummary(summary == null ? "" : summary);
            hit.setChunkIndex(c.getChunkIndex());
            hit.setContent(c.getContent());
            hits.add(hit);
        }
        return hits;
    }

    /** 简单定长切分（字符窗口 + 重叠），可演进为按段落/语义切分 */
    private List<String> split(String content) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());
            chunks.add(content.substring(start, end));
            if (end >= content.length()) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }
        return chunks;
    }

    /** embedding 并归一化：归一化后余弦相似度退化为点积，检索时更快 */
    private float[] embedAndNormalize(String text) {
        Response<Embedding> response = embeddingModel.embed(text);
        float[] vector = response.content().vector();
        double norm = 0;
        for (float v : vector) {
            norm += (double) v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
        return vector;
    }

    private static float dot(float[] a, float[] b) {
        float sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private static byte[] toBytes(float[] vector) {
        ByteBuffer buf = ByteBuffer.allocate(vector.length * Float.BYTES);
        for (float v : vector) {
            buf.putFloat(v);
        }
        return buf.array();
    }

    private static float[] toFloats(byte[] bytes) {
        FloatBuffer buf = ByteBuffer.wrap(bytes).asFloatBuffer();
        float[] vector = new float[buf.remaining()];
        buf.get(vector);
        return vector;
    }
}
