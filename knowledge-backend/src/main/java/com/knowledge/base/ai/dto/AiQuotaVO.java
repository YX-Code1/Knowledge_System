package com.knowledge.base.ai.dto;

import lombok.Data;

/** AI 配额使用情况（每用户每日） */
@Data
public class AiQuotaVO {

    /** 今日已用次数 */
    private long used;

    /** 每日上限 */
    private long dailyLimit;

    /** 今日剩余次数 */
    private long remaining;
}
