package com.knowledge.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.entity.CollectionPurchase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CollectionPurchaseMapper extends BaseMapper<CollectionPurchase> {

    /** 买家是否已购包含该文章的合集（合集即权益；被删合集自动失效） */
    @Select("SELECT COUNT(*) FROM collection_purchase cp " +
            "INNER JOIN collection_article ca ON ca.collection_id = cp.collection_id " +
            "INNER JOIN collection c ON c.id = cp.collection_id AND c.deleted = 0 " +
            "WHERE cp.buyer_id = #{buyerId} AND ca.article_id = #{articleId}")
    long countByArticleAndBuyer(@Param("articleId") Long articleId, @Param("buyerId") Long buyerId);
}
