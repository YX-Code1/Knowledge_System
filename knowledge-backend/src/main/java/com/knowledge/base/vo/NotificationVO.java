package com.knowledge.base.vo;

import com.knowledge.base.entity.Notification;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知
 */
@Data
public class NotificationVO {
    private Long id;

    /** 接收通知的用户ID */
    private Long userId;

    /** 通知类型，如 CATEGORY_CHANGED（分类新增子分类） */
    private String type;

    /** 通知内容 */
    private String content;

    /** 已读状态：0-未读 1-已读 */
    private Integer isRead;

    private LocalDateTime createTime;

    public static NotificationVO from(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setUserId(notification.getUserId());
        vo.setType(notification.getType());
        vo.setContent(notification.getContent());
        vo.setIsRead(notification.getIsRead());
        vo.setCreateTime(notification.getCreateTime());
        return vo;
    }
}
