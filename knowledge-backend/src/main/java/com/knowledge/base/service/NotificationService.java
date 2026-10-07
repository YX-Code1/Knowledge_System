package com.knowledge.base.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.vo.NotificationVO;

public interface NotificationService {
    Page<NotificationVO> getMyNotifications(int page, int size, Long userId);
    Long getUnreadCount(Long userId);
    void markRead(Long id, Long userId);
    void markAllRead(Long userId);
    /** 某分类新增子分类后，通知该分类下文章的作者修改分类 */
    void notifyCategoryAddedChild(Long parentCategoryId, String childCategoryName);
}
