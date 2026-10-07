package com.knowledge.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.entity.ArticleCollection;
import com.knowledge.base.vo.CollectionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CollectionMapper extends BaseMapper<ArticleCollection> {

    /**
     * 合集分页查询（广场 ownerId=null；我的合集 ownerId=当前用户）。
     * purchased：当前买家是否已购该合集（buyerId 为 null 时恒为 0）。
     */
    @Select("<script>" +
            "SELECT c.id, c.user_id, c.name, c.description, c.price, c.create_time, " +
            "u.nickname AS creator_nickname, " +
            "(SELECT COUNT(*) FROM collection_article ca WHERE ca.collection_id = c.id) AS article_count, " +
            "IF(cp.id IS NULL, 0, 1) AS purchased " +
            "FROM collection c " +
            "LEFT JOIN sys_user u ON u.id = c.user_id " +
            "LEFT JOIN collection_purchase cp ON cp.collection_id = c.id AND cp.buyer_id = #{buyerId} " +
            "WHERE c.deleted = 0 " +
            "<if test='ownerId != null'> AND c.user_id = #{ownerId} </if>" +
            "ORDER BY c.create_time DESC" +
            "</script>")
    Page<CollectionVO> selectCollectionPage(Page<?> page,
                                            @Param("buyerId") Long buyerId,
                                            @Param("ownerId") Long ownerId);
}
