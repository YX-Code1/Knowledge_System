package com.knowledge.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /** 统计每个分类下直接挂载的已发布文章数 */
    @Select("SELECT category_id AS categoryId, COUNT(*) AS cnt FROM article " +
            "WHERE deleted = 0 AND status = 1 AND category_id IS NOT NULL GROUP BY category_id")
    List<Map<String, Object>> countArticlesByCategory();
}
