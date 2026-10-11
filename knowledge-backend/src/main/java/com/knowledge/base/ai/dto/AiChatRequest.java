package com.knowledge.base.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * RAG 问答请求。历史消息由前端携带（v1 不落库，最多取最近几条）。
 */
@Data
public class AiChatRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题长度不能超过 500 字")
    private String question;

    /** 最近几轮对话，role 为 user/assistant */
    private List<HistoryMessage> history;

    @Data
    public static class HistoryMessage {
        private String role;
        private String content;
    }
}
