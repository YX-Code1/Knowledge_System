package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.entity.Article;
import com.knowledge.base.entity.Favorite;
import com.knowledge.base.mapper.ArticleMapper;
import com.knowledge.base.mapper.FavoriteMapper;
import com.knowledge.base.service.FavoriteService;
import com.knowledge.base.vo.FavoriteVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public void toggleFavorite(Long articleId, Long userId) {
        Favorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getArticleId, articleId)
                        .eq(Favorite::getUserId, userId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
        } else {
            Favorite favorite = new Favorite();
            favorite.setArticleId(articleId);
            favorite.setUserId(userId);
            favoriteMapper.insert(favorite);
        }
    }

    @Override
    public boolean isFavorited(Long articleId, Long userId) {
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getArticleId, articleId)
                        .eq(Favorite::getUserId, userId));
        return count > 0;
    }

    @Override
    public List<FavoriteVO> getUserFavorites(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));
        List<FavoriteVO> vos = favorites.stream().map(FavoriteVO::from).toList();
        fillArticleTitles(vos);
        return vos;
    }

    /**
     * 批量填充收藏项对应的文章标题
     */
    private void fillArticleTitles(List<FavoriteVO> favorites) {
        if (favorites == null || favorites.isEmpty()) {
            return;
        }
        List<Long> articleIds = favorites.stream().map(FavoriteVO::getArticleId).distinct().toList();
        Map<Long, String> titleMap = articleMapper.selectBatchIds(articleIds).stream()
                .collect(Collectors.toMap(Article::getId, Article::getTitle));
        favorites.forEach(f -> f.setArticleTitle(titleMap.getOrDefault(f.getArticleId(), "文章已删除")));
    }
}
