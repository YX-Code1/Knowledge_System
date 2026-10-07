package com.knowledge.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.Result;
import com.knowledge.base.dto.CollectionArticleRequest;
import com.knowledge.base.dto.CollectionRequest;
import com.knowledge.base.entity.User;
import com.knowledge.base.service.CollectionService;
import com.knowledge.base.vo.ArticleCollectionVO;
import com.knowledge.base.vo.CollectionVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    /** 合集广场（公开浏览） */
    @GetMapping
    public Result<Page<CollectionVO>> getCollections(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "12") int size,
                                                     @AuthenticationPrincipal User user) {
        return Result.success(collectionService.getCollections(page, size, user != null ? user.getId() : null));
    }

    /** 我的合集（登录） */
    @GetMapping("/my")
    public Result<List<CollectionVO>> getMyCollections(@AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            return Result.success(collectionService.getMyCollections(user.getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 合集详情（公开浏览，按当前用户标记 own/purchased） */
    @GetMapping("/{id}")
    public Result<CollectionVO> getCollectionDetail(@PathVariable Long id,
                                                    @AuthenticationPrincipal User user) {
        try {
            return Result.success(collectionService.getCollectionDetail(id, user));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<ArticleCollectionVO> createCollection(@Valid @RequestBody CollectionRequest request,
                                                      @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            return Result.success(collectionService.createCollection(user.getId(), request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> updateCollection(@PathVariable Long id,
                                         @Valid @RequestBody CollectionRequest request,
                                         @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            collectionService.updateCollection(id, user.getId(), request);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCollection(@PathVariable Long id,
                                         @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            collectionService.deleteCollection(id, user.getId());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/articles")
    public Result<Void> addArticle(@PathVariable Long id,
                                   @Valid @RequestBody CollectionArticleRequest request,
                                   @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            collectionService.addArticle(id, user.getId(), request.getArticleId());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/articles/{articleId}")
    public Result<Void> removeArticle(@PathVariable Long id,
                                      @PathVariable Long articleId,
                                      @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            collectionService.removeArticle(id, user.getId(), articleId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 购买合集，返回买家最新余额 */
    @PostMapping("/{id}/purchase")
    public Result<BigDecimal> purchaseCollection(@PathVariable Long id,
                                                 @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            return Result.success(collectionService.purchaseCollection(id, user.getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
