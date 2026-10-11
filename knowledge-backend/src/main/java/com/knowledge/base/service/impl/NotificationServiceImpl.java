package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.entity.Article;
import com.knowledge.base.entity.Category;
import com.knowledge.base.entity.Notification;
import com.knowledge.base.mapper.ArticleMapper;
import com.knowledge.base.mapper.CategoryMapper;
import com.knowledge.base.mapper.NotificationMapper;
import com.knowledge.base.service.NotificationService;
import com.knowledge.base.vo.NotificationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public Page<NotificationVO> getMyNotifications(int page, int size, Long userId) {
        Page<Notification> result = notificationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime));
        return (Page<NotificationVO>) result.convert(NotificationVO::from);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
    }

    @Override
    public void markRead(Long id, Long userId) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该通知");
        }
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
    }

    @Override
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
    }

    @Override
    public void notifyCategoryAddedChild(Long parentCategoryId, String childCategoryName) {
        Category parent = categoryMapper.selectById(parentCategoryId);
        String parentName = parent != null ? parent.getName() : "原分类";

        // 查询直接挂在该分类下的已发布文章，按作者去重后逐一通知
        Set<Long> authorIds = articleMapper.selectList(
                        new LambdaQueryWrapper<Article>()
                                .eq(Article::getCategoryId, parentCategoryId)
                                .eq(Article::getStatus, 1))
                .stream()
                .map(Article::getAuthorId)
                .collect(Collectors.toSet());

        for (Long authorId : authorIds) {
            Notification notification = new Notification();
            notification.setUserId(authorId);
            notification.setType("CATEGORY_CHANGED");
            notification.setContent("你的文章所在分类「" + parentName + "」新增了子分类「" + childCategoryName
                    + "」，请将文章修改到更具体的分类");
            notification.setIsRead(0);
            notificationMapper.insert(notification);
        }
    }

    @Override
    public void notifyCategoryAddedChildForAuthor(Long parentCategoryId, String childCategoryName, Article article) {
        Category parent = categoryMapper.selectById(parentCategoryId);
        String parentName = parent != null ? parent.getName() : "原分类";
        Notification notification = new Notification();
        notification.setUserId(article.getAuthorId());
        notification.setType("CATEGORY_CHANGED");
        notification.setContent(fitContent("你的文章《" + article.getTitle() + "》所在分类「" + parentName
                + "」新增了子分类「" + childCategoryName + "」，AI 未能自动归类，请手动将文章修改到更具体的分类"));
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    @Override
    public void notifyAiRecategorized(Article article, String fromCategoryName, String toCategoryName, String reason) {
        String reasonText = (reason == null || reason.isBlank()) ? "" : "原因：" + reason.trim() + "。";
        Notification notification = new Notification();
        notification.setUserId(article.getAuthorId());
        notification.setType("CATEGORY_CHANGED");
        notification.setContent(fitContent("系统已根据文章内容将《" + article.getTitle() + "》从「" + fromCategoryName
                + "」自动调整到「" + toCategoryName + "」。" + reasonText + "如不合适可自行修改。"));
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    /** 通知内容落库前截断到字段长度（VARCHAR(500)），防止超长插入报错 */
    private String fitContent(String content) {
        return content.length() <= 500 ? content : content.substring(0, 500);
    }
}
