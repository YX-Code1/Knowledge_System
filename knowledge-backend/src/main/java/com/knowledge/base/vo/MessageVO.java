package com.knowledge.base.vo;

import com.knowledge.base.entity.Message;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信
 */
@Data
public class MessageVO {
    private Long id;

    /** 发送者ID */
    private Long senderId;

    /** 接收者ID */
    private Long receiverId;

    /** 消息内容 */
    private String content;

    /** 已读状态：0-未读 1-已读 */
    private Integer isRead;

    private LocalDateTime createTime;

    public static MessageVO from(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
