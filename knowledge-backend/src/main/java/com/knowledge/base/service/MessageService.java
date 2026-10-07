package com.knowledge.base.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.vo.ConversationVO;
import com.knowledge.base.vo.MessageVO;

import java.util.List;

public interface MessageService {
    MessageVO sendMessage(Long senderId, Long receiverId, String content);
    List<ConversationVO> getConversations(Long userId);
    Page<MessageVO> getConversationMessages(Long userId, Long peerId, int page, int size);
    Long getUnreadCount(Long userId);
    /** 拉黑用户（拉黑后双方无法互发私信，历史消息保留） */
    void blockUser(Long userId, Long blockedId);
    /** 解除拉黑 */
    void unblockUser(Long userId, Long blockedId);
}
