package com.knowledge.base.service;

import com.knowledge.base.vo.FavoriteVO;

import java.util.List;

public interface FavoriteService {
    void toggleFavorite(Long articleId, Long userId);
    boolean isFavorited(Long articleId, Long userId);
    List<FavoriteVO> getUserFavorites(Long userId);
}
