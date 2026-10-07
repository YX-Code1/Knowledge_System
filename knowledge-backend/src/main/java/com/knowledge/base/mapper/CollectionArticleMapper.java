package com.knowledge.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.entity.CollectionArticle;
import com.knowledge.base.vo.ArticleSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CollectionArticleMapper extends BaseMapper<CollectionArticle> {

    /** 合集内文章列表（子集字段，join 过滤已删文章） */
    @Select("SELECT a.id, a.title, a.summary, a.price, a.create_time " +
            "FROM collection_article ca " +
            "INNER JOIN article a ON a.id = ca.article_id AND a.deleted = 0 " +
            "WHERE ca.collection_id = #{collectionId} ORDER BY ca.create_time DESC")
    List<ArticleSummaryVO> selectArticlesByCollectionId(@Param("collectionId") Long collectionId);
}
