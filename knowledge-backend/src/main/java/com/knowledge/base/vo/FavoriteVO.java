package com.knowledge.base.vo;

import com.knowledge.base.entity.Favorite;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏
 */
@Data
public class FavoriteVO {
    private Long id;

    private Long articleId;

    private Long userId;

    private LocalDateTime createTime;

    /** 文章标题（查询时填充） */
    private String articleTitle;

    public static FavoriteVO from(Favorite favorite) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(favorite.getId());
        vo.setArticleId(favorite.getArticleId());
        vo.setUserId(favorite.getUserId());
        vo.setCreateTime(favorite.getCreateTime());
        return vo;
    }
}
