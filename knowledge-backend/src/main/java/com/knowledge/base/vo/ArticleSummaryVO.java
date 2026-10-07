package com.knowledge.base.vo;

import com.knowledge.base.entity.Article;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文章摘要（合集内文章列表等子集场景，无正文）
 */
@Data
public class ArticleSummaryVO {
    private Long id;

    private String title;

    private String summary;

    /** 售价，0为免费 */
    private BigDecimal price;

    private LocalDateTime createTime;

    public static ArticleSummaryVO from(Article article) {
        ArticleSummaryVO vo = new ArticleSummaryVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setPrice(article.getPrice());
        vo.setCreateTime(article.getCreateTime());
        return vo;
    }
}
