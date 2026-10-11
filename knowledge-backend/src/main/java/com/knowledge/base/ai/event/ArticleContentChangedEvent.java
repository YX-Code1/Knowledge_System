package com.knowledge.base.ai.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文章内容变化事件（发布/编辑/删除）。
 * 在文章事务内发布，事务提交后由 {@code KnowledgeBaseService} 异步消费，
 * 重建该文章的向量索引。
 */
@Getter
@AllArgsConstructor
public class ArticleContentChangedEvent {

    private final Long articleId;
}
