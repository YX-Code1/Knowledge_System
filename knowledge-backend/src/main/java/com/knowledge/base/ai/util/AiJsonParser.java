package com.knowledge.base.ai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.base.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AI 输出的 JSON 容错解析：
 * 模型偶尔会包裹 markdown 代码块或在 JSON 前后加说明文字，
 * 这里剥掉包裹、截取首尾花括号之间的内容再交给 Jackson。
 * 解析失败一律抛业务异常，由调用方决定如何降级。
 */
@Slf4j
@Component
public class AiJsonParser {

    @Autowired
    private ObjectMapper objectMapper;

    public <T> T parse(String aiOutput, Class<T> type) {
        if (aiOutput == null || aiOutput.isBlank()) {
            throw new BusinessException("AI 输出为空，请重试");
        }
        String json = aiOutput.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException("AI 输出解析失败，请重试");
        }
        try {
            return objectMapper.readValue(json.substring(start, end + 1), type);
        } catch (Exception e) {
            log.warn("AI 输出解析失败，原始输出: {}", json);
            throw new BusinessException("AI 输出解析失败，请重试");
        }
    }

    /** 截断长文本，避免超出模型输入上限 */
    public static String truncate(String text, int maxLength) {
        return text == null || text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
