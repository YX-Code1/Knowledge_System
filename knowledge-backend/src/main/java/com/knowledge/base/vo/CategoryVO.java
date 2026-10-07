package com.knowledge.base.vo;

import com.knowledge.base.entity.Category;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类
 */
@Data
public class CategoryVO {
    private Long id;

    private String name;

    private Long parentId;

    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 文章数（含子分类，查询时填充） */
    private Long articleCount;

    public static CategoryVO from(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setParentId(category.getParentId());
        vo.setSort(category.getSort());
        vo.setCreateTime(category.getCreateTime());
        vo.setUpdateTime(category.getUpdateTime());
        return vo;
    }
}
