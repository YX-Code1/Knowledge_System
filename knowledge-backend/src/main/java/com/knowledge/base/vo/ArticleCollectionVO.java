package com.knowledge.base.vo;

import com.knowledge.base.entity.ArticleCollection;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合集基本信息
 */
@Data
public class ArticleCollectionVO {
    private Long id;

    /** 创建者ID */
    private Long userId;

    /** 合集名称 */
    private String name;

    /** 合集描述 */
    private String description;

    /** 售价，0为免费 */
    private BigDecimal price;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static ArticleCollectionVO from(ArticleCollection collection) {
        ArticleCollectionVO vo = new ArticleCollectionVO();
        vo.setId(collection.getId());
        vo.setUserId(collection.getUserId());
        vo.setName(collection.getName());
        vo.setDescription(collection.getDescription());
        vo.setPrice(collection.getPrice());
        vo.setCreateTime(collection.getCreateTime());
        vo.setUpdateTime(collection.getUpdateTime());
        return vo;
    }
}
