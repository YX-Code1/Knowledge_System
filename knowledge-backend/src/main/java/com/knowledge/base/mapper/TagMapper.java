package com.knowledge.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /** 统计每个标签被多少篇已发布文章使用 */
    @Select("SELECT at.tag_id AS tagId, COUNT(*) AS cnt FROM article_tag at " +
            "INNER JOIN article a ON a.id = at.article_id AND a.deleted = 0 AND a.status = 1 " +
            "GROUP BY at.tag_id")
    List<Map<String, Object>> countArticlesByTag();
}
