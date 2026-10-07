package com.knowledge.base.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.dto.CollectionRequest;
import com.knowledge.base.entity.User;
import com.knowledge.base.vo.ArticleCollectionVO;
import com.knowledge.base.vo.CollectionVO;

import java.math.BigDecimal;
import java.util.List;

public interface CollectionService {
    /** 合集广场分页 */
    Page<CollectionVO> getCollections(int page, int size, Long buyerId);
    /** 我的合集 */
    List<CollectionVO> getMyCollections(Long userId);
    /** 合集详情（含文章列表，按当前用户标记 own/purchased） */
    CollectionVO getCollectionDetail(Long id, User currentUser);
    ArticleCollectionVO createCollection(Long userId, CollectionRequest request);
    void updateCollection(Long id, Long userId, CollectionRequest request);
    void deleteCollection(Long id, Long userId);
    void addArticle(Long id, Long userId, Long articleId);
    void removeArticle(Long id, Long userId, Long articleId);
    /** 购买合集，返回买家最新余额 */
    BigDecimal purchaseCollection(Long id, Long buyerId);
}
