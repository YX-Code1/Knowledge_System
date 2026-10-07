package com.knowledge.base.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 私信会话：与某个用户的往来消息汇总
 */
@Data
public class ConversationVO {
    /** 对方用户ID */
    private Long peerId;
    /** 对方昵称 */
    private String peerNickname;
    /** 对方头像 */
    private String peerAvatar;
    /** 最后一条消息内容 */
    private String lastContent;
    /** 最后一条消息时间 */
    private LocalDateTime lastTime;
    /** 对方发来且未读的消息数 */
    private Long unreadCount;
    /** 当前用户是否已拉黑对方：1-已拉黑 0-未拉黑 */
    private Integer blocked;
}
