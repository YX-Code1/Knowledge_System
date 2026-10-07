package com.knowledge.base.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CollectionArticleRequest {
    @NotNull(message = "文章ID不能为空")
    private Long articleId;
}
