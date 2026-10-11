package com.knowledge.base.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * AI 模型装配：
 * - chat 用 DeepSeek（deepseek-v4-flash），OpenAI 兼容协议，换 baseUrl 即可
 * - embedding 用硅基流动 BAAI/bge-m3（免费、1024 维），同样走 OpenAI 兼容协议
 * <p>
 * 不使用 langchain4j-spring-boot-starter：其当前版本线要求 Spring Boot 3.5+，
 * 本项目 3.2.5 手动装配，两个 Bean 一目了然。
 * <p>
 * key 未配置时 Bean 照常创建（空字符串），调用方在请求前校验并给出友好提示，
 * 应用启动不依赖外部 API 可用性。
 */
@Configuration
@EnableAsync
public class AiConfig {

    /**
     * AI 专用线程池：核心单线程串行执行，对模型提供方的限流友好；
     * 队列有界，任务堆积时直接拒绝（重分类这类任务丢了也只是降级为手动提醒，可接受）。
     */
    @Bean("aiExecutor")
    public Executor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public ChatModel chatModel(
            @Value("${ai.deepseek.api-key:}") String apiKey,
            @Value("${ai.deepseek.base-url:https://api.deepseek.com/v1}") String baseUrl,
            @Value("${ai.deepseek.model:deepseek-v4-flash}") String model,
            @Value("${ai.deepseek.timeout-seconds:60}") long timeoutSeconds) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                // 摘要/标签/分类都是确定性任务，低温减少发散；问答生成也能保持稳重
                .temperature(0.3)
                .maxTokens(2000)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * 流式 chat 模型（RAG 问答用）：LangChain4j 1.x 中流式与非流式是独立类，
     * OpenAiChatModel 不再实现 StreamingChatModel 接口。
     */
    @Bean
    public StreamingChatModel streamingChatModel(
            @Value("${ai.deepseek.api-key:}") String apiKey,
            @Value("${ai.deepseek.base-url:https://api.deepseek.com/v1}") String baseUrl,
            @Value("${ai.deepseek.model:deepseek-v4-flash}") String model,
            @Value("${ai.deepseek.timeout-seconds:60}") long timeoutSeconds) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${ai.siliconflow.api-key:}") String apiKey,
            @Value("${ai.siliconflow.base-url:https://api.siliconflow.cn/v1}") String baseUrl,
            @Value("${ai.siliconflow.embedding-model:BAAI/bge-m3}") String model) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .build();
    }
}
