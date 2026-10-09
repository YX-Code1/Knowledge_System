package com.knowledge.base.controller;

import com.knowledge.base.common.Result;
import com.knowledge.base.entity.User;
import com.knowledge.base.service.FavoriteService;
import com.knowledge.base.vo.FavoriteVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping("/toggle/{articleId}")
    public Result<Void> toggleFavorite(@PathVariable Long articleId,
                                       @AuthenticationPrincipal User user) {
        favoriteService.toggleFavorite(articleId, user.getId());
        return Result.success();
    }

    @GetMapping("/check/{articleId}")
    public Result<Boolean> isFavorited(@PathVariable Long articleId,
                                       @AuthenticationPrincipal User user) {
        return Result.success(favoriteService.isFavorited(articleId, user.getId()));
    }

    @GetMapping("/my")
    public Result<List<FavoriteVO>> getMyFavorites(@AuthenticationPrincipal User user) {
        return Result.success(favoriteService.getUserFavorites(user.getId()));
    }
}
