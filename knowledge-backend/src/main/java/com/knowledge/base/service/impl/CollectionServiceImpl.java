package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.dto.CollectionRequest;
import com.knowledge.base.entity.*;
import com.knowledge.base.mapper.*;
import com.knowledge.base.service.CollectionService;
import com.knowledge.base.vo.ArticleCollectionVO;
import com.knowledge.base.vo.CollectionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CollectionServiceImpl implements CollectionService {

    @Autowired
    private CollectionMapper collectionMapper;

    @Autowired
    private CollectionArticleMapper collectionArticleMapper;

    @Autowired
    private CollectionPurchaseMapper collectionPurchaseMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    @Override
    public Page<CollectionVO> getCollections(int page, int size, Long buyerId) {
        return collectionMapper.selectCollectionPage(new Page<>(page, size), buyerId, null);
    }

    @Override
    public List<CollectionVO> getMyCollections(Long userId) {
        return collectionMapper.selectCollectionPage(new Page<>(1, 100), userId, userId).getRecords();
    }

    @Override
    public CollectionVO getCollectionDetail(Long id, User currentUser) {
        ArticleCollection collection = collectionMapper.selectById(id);
        if (collection == null) {
            throw new BusinessException("合集不存在");
        }
        CollectionVO vo = new CollectionVO();
        vo.setId(collection.getId());
        vo.setUserId(collection.getUserId());
        vo.setName(collection.getName());
        vo.setDescription(collection.getDescription());
        vo.setPrice(collection.getPrice());
        vo.setCreateTime(collection.getCreateTime());

        User creator = userMapper.selectById(collection.getUserId());
        vo.setCreatorNickname(creator != null ? creator.getNickname() : "用户已注销");

        vo.setArticleCount(collectionArticleMapper.selectCount(
                new LambdaQueryWrapper<CollectionArticle>()
                        .eq(CollectionArticle::getCollectionId, id)));
        vo.setArticles(collectionArticleMapper.selectArticlesByCollectionId(id));

        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        vo.setOwn(currentUserId != null && currentUserId.equals(collection.getUserId()));
        boolean purchased = currentUserId != null && collectionPurchaseMapper.selectCount(
                new LambdaQueryWrapper<CollectionPurchase>()
                        .eq(CollectionPurchase::getCollectionId, id)
                        .eq(CollectionPurchase::getBuyerId, currentUserId)) > 0;
        vo.setPurchased(purchased ? 1 : 0);
        return vo;
    }

    @Override
    public ArticleCollectionVO createCollection(Long userId, CollectionRequest request) {
        ArticleCollection collection = new ArticleCollection();
        collection.setUserId(userId);
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        collection.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        collectionMapper.insert(collection);
        return ArticleCollectionVO.from(collection);
    }

    @Override
    public void updateCollection(Long id, Long userId, CollectionRequest request) {
        ArticleCollection collection = getOwnCollection(id, userId);
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        collection.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        collectionMapper.updateById(collection);
    }

    @Override
    @Transactional
    public void deleteCollection(Long id, Long userId) {
        getOwnCollection(id, userId);
        collectionMapper.deleteById(id);
        // 物理删除关联（合集已删，关联无意义）；购买记录保留留痕
        collectionArticleMapper.delete(new LambdaQueryWrapper<CollectionArticle>()
                .eq(CollectionArticle::getCollectionId, id));
    }

    @Override
    public void addArticle(Long id, Long userId, Long articleId) {
        getOwnCollection(id, userId);
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new BusinessException("只能添加自己的文章");
        }
        if (collectionArticleMapper.selectCount(new LambdaQueryWrapper<CollectionArticle>()
                .eq(CollectionArticle::getCollectionId, id)
                .eq(CollectionArticle::getArticleId, articleId)) > 0) {
            throw new BusinessException("该文章已在合集中");
        }
        CollectionArticle ca = new CollectionArticle();
        ca.setCollectionId(id);
        ca.setArticleId(articleId);
        collectionArticleMapper.insert(ca);
    }

    @Override
    public void removeArticle(Long id, Long userId, Long articleId) {
        getOwnCollection(id, userId);
        collectionArticleMapper.delete(new LambdaQueryWrapper<CollectionArticle>()
                .eq(CollectionArticle::getCollectionId, id)
                .eq(CollectionArticle::getArticleId, articleId));
    }

    @Override
    @Transactional
    public BigDecimal purchaseCollection(Long id, Long buyerId) {
        ArticleCollection collection = collectionMapper.selectById(id);
        if (collection == null) throw new BusinessException("合集不存在");
        if (collection.getPrice() == null || collection.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("该合集为免费合集，无需购买");
        }
        if (collection.getUserId().equals(buyerId)) {
            throw new BusinessException("不能购买自己的合集");
        }
        if (collectionPurchaseMapper.selectCount(new LambdaQueryWrapper<CollectionPurchase>()
                .eq(CollectionPurchase::getCollectionId, id)
                .eq(CollectionPurchase::getBuyerId, buyerId)) > 0) {
            throw new BusinessException("您已购买过该合集");
        }
        if (collectionArticleMapper.selectCount(new LambdaQueryWrapper<CollectionArticle>()
                .eq(CollectionArticle::getCollectionId, id)) == 0) {
            throw new BusinessException("该合集暂无文章，无法购买");
        }

        if (userMapper.deductBalanceIfEnough(buyerId, collection.getPrice()) == 0) {
            throw new BusinessException("余额不足，请先充值");
        }
        userMapper.addBalance(collection.getUserId(), collection.getPrice());

        CollectionPurchase purchase = new CollectionPurchase();
        purchase.setCollectionId(id);
        purchase.setBuyerId(buyerId);
        purchase.setAmount(collection.getPrice());
        collectionPurchaseMapper.insert(purchase);

        insertBalanceLog(buyerId, collection.getPrice().negate(), "PURCHASE", "购买合集《" + collection.getName() + "》");
        insertBalanceLog(collection.getUserId(), collection.getPrice(), "INCOME", "合集《" + collection.getName() + "》售出");

        return userMapper.selectById(buyerId).getBalance();
    }

    private void insertBalanceLog(Long userId, BigDecimal change, String type, String remark) {
        BalanceLog log = new BalanceLog();
        log.setUserId(userId);
        log.setChangeAmount(change);
        log.setBalanceAfter(userMapper.selectById(userId).getBalance());
        log.setType(type);
        log.setRemark(remark);
        balanceLogMapper.insert(log);
    }

    /**
     * 获取属于当前用户的合集（非创建者抛异常）
     */
    private ArticleCollection getOwnCollection(Long id, Long userId) {
        ArticleCollection collection = collectionMapper.selectById(id);
        if (collection == null) {
            throw new BusinessException("合集不存在");
        }
        if (!collection.getUserId().equals(userId)) {
            throw new BusinessException("只能操作自己的合集");
        }
        return collection;
    }
}
