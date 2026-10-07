package com.knowledge.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    // 手写 SQL 需自带 deleted=0（与 MyBatis-Plus 逻辑删除约定一致）
    @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    void incrementViewCount(@Param("id") Long id);

    @Update("UPDATE article SET like_count = like_count + #{delta} WHERE id = #{id} AND deleted = 0")
    void updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE article SET comment_count = comment_count + #{delta} WHERE id = #{id} AND deleted = 0")
    void updateCommentCount(@Param("id") Long id, @Param("delta") int delta);

    @Select("SELECT IFNULL(SUM(view_count),0) FROM article WHERE deleted = 0")
    long getTotalViewCount();
}
