package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.entity.Message;
import com.knowledge.base.entity.User;
import com.knowledge.base.entity.UserBlock;
import com.knowledge.base.mapper.MessageMapper;
import com.knowledge.base.mapper.UserBlockMapper;
import com.knowledge.base.mapper.UserMapper;
import com.knowledge.base.service.MessageService;
import com.knowledge.base.vo.ConversationVO;
import com.knowledge.base.vo.MessageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserBlockMapper userBlockMapper;

    @Override
    public MessageVO sendMessage(Long senderId, Long receiverId, String content) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException("不能给自己发送私信");
        }
        if (userMapper.selectById(receiverId) == null) {
            throw new BusinessException("用户不存在");
        }
        // 拉黑拦截：任一方向拉黑均不可互发（历史消息保留可见）
        if (isBlocked(receiverId, senderId)) {
            throw new BusinessException("对方已将你拉黑，无法发送私信");
        }
        if (isBlocked(senderId, receiverId)) {
            throw new BusinessException("你已拉黑该用户，请先解除拉黑");
        }
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setIsRead(0);
        messageMapper.insert(message);
        return MessageVO.from(message);
    }

    @Override
    public List<ConversationVO> getConversations(Long userId) {
        List<Message> lastMessages = messageMapper.selectLastMessages(userId);
        if (lastMessages.isEmpty()) {
            return List.of();
        }

        // 对方用户ID集合，批量查昵称头像
        List<Long> peerIds = lastMessages.stream()
                .map(m -> m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId())
                .distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(peerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 每个发送者发来的未读数
        Map<Long, Long> unreadMap = new HashMap<>();
        for (Map<String, Object> row : messageMapper.countUnreadBySender(userId)) {
            unreadMap.put(toLong(row.get("senderId")), toLong(row.get("cnt")));
        }

        // 我拉黑了的对方ID集合
        Set<Long> blockedIds = new HashSet<>();
        if (!peerIds.isEmpty()) {
            blockedIds = userBlockMapper.selectList(new LambdaQueryWrapper<UserBlock>()
                            .eq(UserBlock::getUserId, userId)
                            .in(UserBlock::getBlockedId, peerIds))
                    .stream().map(UserBlock::getBlockedId).collect(Collectors.toSet());
        }

        List<ConversationVO> conversations = new ArrayList<>();
        for (Message m : lastMessages) {
            Long peerId = m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId();
            User peer = userMap.get(peerId);
            ConversationVO vo = new ConversationVO();
            vo.setPeerId(peerId);
            vo.setPeerNickname(peer != null ? peer.getNickname() : "用户已注销");
            vo.setPeerAvatar(peer != null ? peer.getAvatar() : null);
            vo.setLastContent(m.getContent());
            vo.setLastTime(m.getCreateTime());
            vo.setUnreadCount(unreadMap.getOrDefault(peerId, 0L));
            vo.setBlocked(blockedIds.contains(peerId) ? 1 : 0);
            conversations.add(vo);
        }
        return conversations;
    }

    @Override
    public Page<MessageVO> getConversationMessages(Long userId, Long peerId, int page, int size) {
        // 倒序分页保证"最新一页"语义正确，返回前反转成正序
        Page<Message> result = messageMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Message>()
                        .and(w -> w
                                .eq(Message::getSenderId, userId).eq(Message::getReceiverId, peerId)
                                .or()
                                .eq(Message::getSenderId, peerId).eq(Message::getReceiverId, userId))
                        .orderByDesc(Message::getCreateTime));

        // 打开会话即视为已读（对方发来的部分）
        messageMapper.markConversationRead(userId, peerId);

        Collections.reverse(result.getRecords());
        return (Page<MessageVO>) result.convert(MessageVO::from);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getReceiverId, userId)
                        .eq(Message::getIsRead, 0));
    }

    @Override
    public void blockUser(Long userId, Long blockedId) {
        if (userId.equals(blockedId)) {
            throw new BusinessException("不能拉黑自己");
        }
        if (isBlocked(userId, blockedId)) {
            throw new BusinessException("已拉黑该用户");
        }
        UserBlock block = new UserBlock();
        block.setUserId(userId);
        block.setBlockedId(blockedId);
        userBlockMapper.insert(block);
    }

    @Override
    public void unblockUser(Long userId, Long blockedId) {
        userBlockMapper.delete(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, userId)
                .eq(UserBlock::getBlockedId, blockedId));
    }

    /** userId 是否拉黑了 blockedId */
    private boolean isBlocked(Long userId, Long blockedId) {
        return userBlockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, userId)
                .eq(UserBlock::getBlockedId, blockedId)) > 0;
    }

    private long toLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }
}
