package com.knowledge.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.Result;
import com.knowledge.base.dto.ArticleRequest;
import com.knowledge.base.entity.User;
import com.knowledge.base.service.ArticleService;
import com.knowledge.base.vo.ArticleVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @GetMapping
    public Result<Page<ArticleVO>> getArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword) {
        Page<ArticleVO> articles = articleService.getArticles(page, size, categoryId, tagId, keyword);
        return Result.success(articles);
    }

    @GetMapping("/{id}")
    public Result<ArticleVO> getArticle(@PathVariable Long id,
                                      @AuthenticationPrincipal User user) {
        articleService.incrementViewCount(id);
        // 按当前登录用户权限返回正文（未登录传 null，付费文章正文为空）
        ArticleVO article = articleService.getArticleById(id, user);
        return Result.success(article);
    }

    /** 我的文章（供合集添加文章选择） */
    @GetMapping("/my")
    public Result<Page<ArticleVO>> getMyArticles(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        return Result.success(articleService.getMyArticles(user.getId(), page, size));
    }

    @PostMapping
    public Result<ArticleVO> createArticle(@Valid @RequestBody ArticleRequest request,
                                         @AuthenticationPrincipal User user) {
        ArticleVO article = articleService.createArticle(request, user.getId());
        return Result.success(article);
    }

    @PutMapping("/{id}")
    public Result<ArticleVO> updateArticle(@PathVariable Long id,
                                         @Valid @RequestBody ArticleRequest request,
                                         @AuthenticationPrincipal User user) {
        ArticleVO article = articleService.updateArticle(id, request, user);
        return Result.success(article);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id,
                                      @AuthenticationPrincipal User user) {
        articleService.deleteArticle(id, user);
        return Result.success();
    }

    /** 购买付费文章，返回买家最新余额 */
    @PostMapping("/{id}/purchase")
    public Result<java.math.BigDecimal> purchaseArticle(@PathVariable Long id,
                                                        @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        return Result.success(articleService.purchaseArticle(id, user.getId()));
    }

    @PostMapping("/{id}/like")
    public Result<Void> likeArticle(@PathVariable Long id, @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        articleService.likeArticle(id, user.getId());
        return Result.success();
    }
}
