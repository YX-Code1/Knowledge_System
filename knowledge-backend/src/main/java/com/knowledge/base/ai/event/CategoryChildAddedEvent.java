package com.knowledge.base.ai.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分类新增子分类事件。
 * 在分类创建/移动事务内发布，事务提交后（AFTER_COMMIT）由
 * {@code AiCategorizationService} 异步消费，触发 AI 自动重分类。
 */
@Getter
@AllArgsConstructor
public class CategoryChildAddedEvent {

    /** 父分类 id（其下新增了子分类） */
    private final Long parentCategoryId;

    /** 新增子分类名称（仅用于降级提醒文案） */
    private final String childCategoryName;
}
