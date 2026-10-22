package com.knowledge.base.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.dto.ArticleRequest;
import com.knowledge.base.entity.User;
import com.knowledge.base.vo.ArticleVO;

import java.math.BigDecimal;

public interface ArticleService {
    /**
     * 分页查询文章
     *
     * @param sort     latest=按发布时间，hot=按浏览量，其他/null=保持原有按点赞数排序
     * @param freeOnly 为 true 时只返回免费文章
     */
    Page<ArticleVO> getArticles(int page, int size, Long categoryId, Long tagId, String keyword,
                                String sort, Boolean freeOnly);
    /** 按当前用户权限返回文章（付费未购买时正文为空且 locked=true，标题/摘要正常展示） */
    ArticleVO getArticleById(Long id, User currentUser);
    Page<ArticleVO> getMyArticles(Long authorId, int page, int size);
    ArticleVO createArticle(ArticleRequest request, Long authorId);
    ArticleVO updateArticle(Long id, ArticleRequest request, User currentUser);
    void deleteArticle(Long id, User currentUser);
    /** 购买付费文章，返回买家最新余额 */
    BigDecimal purchaseArticle(Long id, Long buyerId);
    void likeArticle(Long id, Long userId);
    void incrementViewCount(Long id);
}
