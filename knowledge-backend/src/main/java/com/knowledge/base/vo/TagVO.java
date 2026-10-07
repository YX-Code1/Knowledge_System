package com.knowledge.base.vo;

import com.knowledge.base.entity.Tag;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签
 */
@Data
public class TagVO {
    private Long id;

    private String name;

    private LocalDateTime createTime;

    /** 使用该标签的文章数（查询时填充） */
    private Long articleCount;

    public static TagVO from(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setCreateTime(tag.getCreateTime());
        return vo;
    }
}
