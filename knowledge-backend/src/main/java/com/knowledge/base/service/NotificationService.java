package com.knowledge.base.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.entity.Article;
import com.knowledge.base.vo.NotificationVO;

public interface NotificationService {
    Page<NotificationVO> getMyNotifications(int page, int size, Long userId);
    Long getUnreadCount(Long userId);
    void markRead(Long id, Long userId);
    void markAllRead(Long userId);
    /** 某分类新增子分类后，通知该分类下文章的作者修改分类 */
    void notifyCategoryAddedChild(Long parentCategoryId, String childCategoryName);
    /** AI 自动重分类失败时，对单篇文章作者的降级提醒 */
    void notifyCategoryAddedChildForAuthor(Long parentCategoryId, String childCategoryName, Article article);
    /** AI 自动重分类成功后，通知作者改成了什么分类、为什么 */
    void notifyAiRecategorized(Article article, String fromCategoryName, String toCategoryName, String reason);
}
