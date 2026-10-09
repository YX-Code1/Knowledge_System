package com.knowledge.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发表评论请求。
 * 不直接绑定 Comment 实体：userId/创建时间等字段由服务端设置，防止前端提交篡改（mass assignment）。
 */
@Data
public class CommentRequest {

    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    /** 父评论ID，回复评论时使用，可空 */
    private Long parentId;
}
