package com.knowledge.base.vo;

import com.knowledge.base.entity.Article;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章（tags/liked/locked/purchased 为查询时填充的展示字段）
 */
@Data
public class ArticleVO {
    private Long id;

    private String title;

    private String summary;

    /** 售价，0为免费 */
    private BigDecimal price;

    private String content;

    private Long categoryId;

    private Long authorId;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 文章标签（查询时填充） */
    private List<TagVO> tags;

    /** 当前用户是否已点赞（查询时填充） */
    private Boolean liked;

    /** 正文是否对当前用户锁定（付费未购买） */
    private Boolean locked;

    /** 当前用户是否已购买 */
    private Boolean purchased;

    public static ArticleVO from(Article article) {
        ArticleVO vo = new ArticleVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setPrice(article.getPrice());
        vo.setContent(article.getContent());
        vo.setCategoryId(article.getCategoryId());
        vo.setAuthorId(article.getAuthorId());
        vo.setViewCount(article.getViewCount());
        vo.setLikeCount(article.getLikeCount());
        vo.setCommentCount(article.getCommentCount());
        vo.setStatus(article.getStatus());
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());
        return vo;
    }
}
