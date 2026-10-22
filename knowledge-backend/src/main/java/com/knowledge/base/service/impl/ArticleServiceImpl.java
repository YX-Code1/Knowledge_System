package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.ai.event.ArticleContentChangedEvent;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.dto.ArticleRequest;
import com.knowledge.base.entity.Article;
import com.knowledge.base.entity.ArticleLike;
import com.knowledge.base.entity.ArticlePurchase;
import com.knowledge.base.entity.ArticleTag;
import com.knowledge.base.entity.BalanceLog;
import com.knowledge.base.entity.Category;
import com.knowledge.base.entity.CollectionArticle;
import com.knowledge.base.entity.Comment;
import com.knowledge.base.entity.Favorite;
import com.knowledge.base.entity.Tag;
import com.knowledge.base.entity.User;
import com.knowledge.base.mapper.ArticleLikeMapper;
import com.knowledge.base.mapper.ArticleMapper;
import com.knowledge.base.mapper.ArticlePurchaseMapper;
import com.knowledge.base.mapper.ArticleTagMapper;
import com.knowledge.base.mapper.BalanceLogMapper;
import com.knowledge.base.mapper.CategoryMapper;
import com.knowledge.base.mapper.CollectionArticleMapper;
import com.knowledge.base.mapper.CollectionPurchaseMapper;
import com.knowledge.base.mapper.CommentMapper;
import com.knowledge.base.mapper.FavoriteMapper;
import com.knowledge.base.mapper.TagMapper;
import com.knowledge.base.mapper.UserMapper;
import com.knowledge.base.service.ArticleService;
import com.knowledge.base.vo.ArticleVO;
import com.knowledge.base.vo.TagVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private ArticlePurchaseMapper articlePurchaseMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private CollectionArticleMapper collectionArticleMapper;

    @Autowired
    private CollectionPurchaseMapper collectionPurchaseMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    @Override
    public Page<ArticleVO> getArticles(int page, int size, Long categoryId, Long tagId, String keyword,
                                       String sort, Boolean freeOnly) {
        Page<Article> articlePage = new Page<>(page, size);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        if (categoryId != null) {
            // 父分类筛选包含所有子分类
            wrapper.in(Article::getCategoryId, collectCategoryIds(categoryId));
        }
        if (tagId != null) {
            // 标签筛选在 SQL 层完成（先取文章ID，保证分页正确）
            List<Long> articleIds = articleTagMapper.selectList(
                            new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, tagId))
                    .stream().map(ArticleTag::getArticleId).toList();
            if (articleIds.isEmpty()) {
                return new Page<>(page, size);
            }
            wrapper.in(Article::getId, articleIds);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Article::getTitle, keyword).or().like(Article::getSummary, keyword));
        }
        if (Boolean.TRUE.equals(freeOnly)) {
            // 免费文章：price 为 NULL 或 0
            wrapper.and(w -> w.isNull(Article::getPrice).or().le(Article::getPrice, BigDecimal.ZERO));
        }
        wrapper.eq(Article::getStatus, 1);
        if ("latest".equals(sort)) {
            wrapper.orderByDesc(Article::getCreateTime).orderByDesc(Article::getId);
        } else if ("hot".equals(sort)) {
            wrapper.orderByDesc(Article::getViewCount).orderByDesc(Article::getLikeCount);
        } else {
            // 未传 sort 时保持原有默认排序，兼容旧调用
            wrapper.orderByDesc(Article::getLikeCount);
        }

        Page<Article> result = articleMapper.selectPage(articlePage, wrapper);
        // 列表不返回正文，防止付费内容泄露，同时减小响应体
        result.getRecords().forEach(a -> a.setContent(null));
        Page<ArticleVO> voPage = (Page<ArticleVO>) result.convert(ArticleVO::from);
        fillArticleTags(voPage.getRecords());
        return voPage;
    }

    /**
     * 收集某分类及其全部后代分类的 ID
     */
    private List<Long> collectCategoryIds(Long rootId) {
        List<Category> all = categoryMapper.selectList(null);
        Map<Long, List<Category>> children = new HashMap<>();
        for (Category c : all) {
            children.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
        }
        List<Long> ids = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            ids.add(id);
            for (Category c : children.getOrDefault(id, List.of())) {
                queue.add(c.getId());
            }
        }
        return ids;
    }

    @Override
    public ArticleVO getArticleById(Long id, User currentUser) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            return null;
        }
        ArticleVO vo = ArticleVO.from(article);
        fillArticleTags(List.of(vo));
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        vo.setLiked(hasLiked(id, currentUserId));

        // 付费内容权限判定：免费/作者/管理员/已购文章/已购含该文章的合集 可见全文
        boolean isAuthor = currentUserId != null && currentUserId.equals(article.getAuthorId());
        boolean isAdmin = currentUser != null
                && ("ADMIN".equals(currentUser.getRole()) || "SUPER_ADMIN".equals(currentUser.getRole()));
        boolean free = article.getPrice() == null
                || article.getPrice().compareTo(BigDecimal.ZERO) <= 0;
        boolean bought = currentUserId != null && articlePurchaseMapper.selectCount(
                new LambdaQueryWrapper<ArticlePurchase>()
                        .eq(ArticlePurchase::getArticleId, id)
                        .eq(ArticlePurchase::getBuyerId, currentUserId)) > 0;
        boolean covered = currentUserId != null
                && collectionPurchaseMapper.countByArticleAndBuyer(id, currentUserId) > 0;

        if (free || isAuthor || isAdmin || bought || covered) {
            vo.setLocked(false);
            vo.setPurchased(bought || covered);
        } else {
            // 付费未购买：仅正文锁定，标题/摘要/标签正常展示
            vo.setContent(null);
            vo.setLocked(true);
            vo.setPurchased(false);
        }
        return vo;
    }

    @Override
    public Page<ArticleVO> getMyArticles(Long authorId, int page, int size) {
        Page<Article> result = articleMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getAuthorId, authorId)
                        .orderByDesc(Article::getCreateTime));
        result.getRecords().forEach(a -> a.setContent(null));
        return (Page<ArticleVO>) result.convert(ArticleVO::from);
    }

    /**
     * 批量填充文章的标签列表
     */
    private void fillArticleTags(List<ArticleVO> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        List<Long> articleIds = articles.stream().map(ArticleVO::getId).toList();
        Map<Long, List<Long>> articleTagMap = articleTagMapper.selectList(
                        new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds))
                .stream()
                .collect(Collectors.groupingBy(ArticleTag::getArticleId,
                        Collectors.mapping(ArticleTag::getTagId, Collectors.toList())));

        List<Long> tagIds = articleTagMap.values().stream().flatMap(List::stream).distinct().toList();
        Map<Long, Tag> tagMap = tagIds.isEmpty() ? Map.of() :
                tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getId, tagIds))
                        .stream().collect(Collectors.toMap(Tag::getId, t -> t));

        articles.forEach(a -> a.setTags(
                articleTagMap.getOrDefault(a.getId(), List.of()).stream()
                        .map(tagMap::get).filter(java.util.Objects::nonNull).map(TagVO::from).toList()));
    }

    @Override
    @Transactional
    public ArticleVO createArticle(ArticleRequest request, Long authorId) {
        validateLeafCategory(request.getCategoryId());
        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        // 价格为空视为免费
        article.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        article.setCategoryId(request.getCategoryId());
        article.setAuthorId(authorId);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setStatus(1);
        articleMapper.insert(article);

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            for (Long tagId : request.getTagIds()) {
                ArticleTag at = new ArticleTag();
                at.setArticleId(article.getId());
                at.setTagId(tagId);
                articleTagMapper.insert(at);
            }
        }

        // 事务提交后异步重建该文章的向量索引，失败不影响发布
        eventPublisher.publishEvent(new ArticleContentChangedEvent(article.getId()));

        return ArticleVO.from(article);
    }

    @Override
    @Transactional
    public ArticleVO updateArticle(Long id, ArticleRequest request, User currentUser) {
        Article article = articleMapper.selectById(id);
        if (article == null) throw new BusinessException("文章不存在");
        checkAuthorOrAdmin(article, currentUser);

        // 仅当分类发生变化时才校验（老文章改标题不受叶子限制影响）
        if (!request.getCategoryId().equals(article.getCategoryId())) {
            validateLeafCategory(request.getCategoryId());
        }

        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        article.setCategoryId(request.getCategoryId());
        articleMapper.updateById(article);

        if (request.getTagIds() != null) {
            articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                    .eq(ArticleTag::getArticleId, id));
            for (Long tagId : request.getTagIds()) {
                ArticleTag at = new ArticleTag();
                at.setArticleId(id);
                at.setTagId(tagId);
                articleTagMapper.insert(at);
            }
        }

        // 事务提交后异步重建该文章的向量索引，失败不影响保存
        eventPublisher.publishEvent(new ArticleContentChangedEvent(id));

        return ArticleVO.from(article);
    }

    @Override
    @Transactional
    public void deleteArticle(Long id, User currentUser) {
        Article article = articleMapper.selectById(id);
        if (article == null) throw new BusinessException("文章不存在");
        checkAuthorOrAdmin(article, currentUser);

        // 文章本身逻辑删除（@TableLogic），同事务内级联清理关联数据，避免留下脏数据
        articleMapper.deleteById(id);
        // 评论带 deleted 字段，同样走逻辑删除
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getArticleId, id));
        // 收藏 / 点赞 / 文章标签关联、合集收录无 deleted 字段，物理删除
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getArticleId, id));
        articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getArticleId, id));
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
        collectionArticleMapper.delete(new LambdaQueryWrapper<CollectionArticle>().eq(CollectionArticle::getArticleId, id));
        // 注意：ArticlePurchase / BalanceLog 属于资金流水，删除文章不删购买记录（保留审计痕迹，
        // 也不做退款——余额已结算给作者，退款需独立的对账流程，当前产品决策为不退款）

        // 事务提交后异步清空该文章的向量索引，失败不影响删除
        eventPublisher.publishEvent(new ArticleContentChangedEvent(id));
    }

    @Override
    @Transactional
    public BigDecimal purchaseArticle(Long id, Long buyerId) {
        Article article = articleMapper.selectById(id);
        if (article == null) throw new BusinessException("文章不存在");
        if (article.getPrice() == null || article.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("该文章为免费文章，无需购买");
        }
        if (article.getAuthorId().equals(buyerId)) {
            throw new BusinessException("不能购买自己的文章");
        }
        if (articlePurchaseMapper.selectCount(new LambdaQueryWrapper<ArticlePurchase>()
                .eq(ArticlePurchase::getArticleId, id)
                .eq(ArticlePurchase::getBuyerId, buyerId)) > 0) {
            throw new BusinessException("您已购买过该文章");
        }
        if (collectionPurchaseMapper.countByArticleAndBuyer(id, buyerId) > 0) {
            throw new BusinessException("您已通过购买合集获得该文章，无需重复购买");
        }

        // 原子扣款，0 行即余额不足
        if (userMapper.deductBalanceIfEnough(buyerId, article.getPrice()) == 0) {
            throw new BusinessException("余额不足，请先充值");
        }
        // 平台 0% 抽成，全归作者
        userMapper.addBalance(article.getAuthorId(), article.getPrice());

        ArticlePurchase purchase = new ArticlePurchase();
        purchase.setArticleId(id);
        purchase.setBuyerId(buyerId);
        purchase.setAmount(article.getPrice());
        articlePurchaseMapper.insert(purchase);

        insertBalanceLog(buyerId, article.getPrice().negate(), "PURCHASE", "购买文章《" + article.getTitle() + "》");
        insertBalanceLog(article.getAuthorId(), article.getPrice(), "INCOME", "文章《" + article.getTitle() + "》售出");

        return userMapper.selectById(buyerId).getBalance();
    }

    /**
     * 记录一条余额流水（余额后值在同事务内读取）
     */
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
     * 仅作者或管理员可操作文章
     */
    private void checkAuthorOrAdmin(Article article, User currentUser) {
        if (currentUser == null) {
            throw new BusinessException("请先登录");
        }
        boolean isAdmin = "ADMIN".equals(currentUser.getRole()) || "SUPER_ADMIN".equals(currentUser.getRole());
        if (!currentUser.getId().equals(article.getAuthorId()) && !isAdmin) {
            throw new BusinessException("只能操作自己的文章");
        }
    }

    /**
     * 校验文章只能挂到叶子分类（没有子分类的分类）下
     */
    private void validateLeafCategory(Long categoryId) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BusinessException("分类不存在");
        }
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, categoryId));
        if (childCount > 0) {
            throw new BusinessException("只能选择叶子分类（该分类下已有子分类）");
        }
    }

    @Override
    @Transactional
    public void likeArticle(Long id, Long userId) {
        if (hasLiked(id, userId)) {
            // 已点过赞，再点一次取消点赞
            // 只有真正删除了点赞行才回写计数，防止并发重复取消导致 like_count 漂移
            int rows = articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getArticleId, id)
                    .eq(ArticleLike::getUserId, userId));
            if (rows > 0) {
                articleMapper.updateLikeCount(id, -1);
            }
        } else {
            ArticleLike like = new ArticleLike();
            like.setArticleId(id);
            like.setUserId(userId);
            try {
                articleLikeMapper.insert(like);
            } catch (DuplicateKeyException e) {
                // 并发点赞：唯一键兜底，对方已点过，本次视为成功但不重复计数
                return;
            }
            articleMapper.updateLikeCount(id, 1);
        }
    }

    /**
     * 判断用户是否已点赞该文章
     */
    private boolean hasLiked(Long articleId, Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId));
        return count != null && count > 0;
    }

    /**
     * 从安全上下文取当前登录用户 ID，未登录返回 null
     */
    private Long getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    @Override
    public void incrementViewCount(Long id) {
        articleMapper.incrementViewCount(id);
    }
}
