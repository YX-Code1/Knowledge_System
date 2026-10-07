package com.knowledge.base.vo;

import com.knowledge.base.entity.Comment;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论
 */
@Data
public class CommentVO {
    private Long id;

    private Long articleId;

    private Long userId;

    private String content;

    private Long parentId;

    private LocalDateTime createTime;

    public static CommentVO from(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setArticleId(comment.getArticleId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
