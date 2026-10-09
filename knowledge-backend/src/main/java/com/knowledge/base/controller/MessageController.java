package com.knowledge.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.Result;
import com.knowledge.base.dto.MessageRequest;
import com.knowledge.base.entity.User;
import com.knowledge.base.service.MessageService;
import com.knowledge.base.vo.ConversationVO;
import com.knowledge.base.vo.MessageVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping
    public Result<MessageVO> sendMessage(@Valid @RequestBody MessageRequest request,
                                       @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        MessageVO message = messageService.sendMessage(user.getId(), request.getReceiverId(), request.getContent());
        return Result.success(message);
    }

    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getConversations(@AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        return Result.success(messageService.getConversations(user.getId()));
    }

    @GetMapping("/conversation/{peerId}")
    public Result<Page<MessageVO>> getConversationMessages(@PathVariable Long peerId,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "50") int size,
                                                         @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        return Result.success(messageService.getConversationMessages(user.getId(), peerId, page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        return Result.success(messageService.getUnreadCount(user.getId()));
    }

    /** 拉黑用户 */
    @PostMapping("/block/{userId}")
    public Result<Void> blockUser(@PathVariable Long userId, @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        messageService.blockUser(user.getId(), userId);
        return Result.success();
    }

    /** 解除拉黑 */
    @DeleteMapping("/block/{userId}")
    public Result<Void> unblockUser(@PathVariable Long userId, @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        messageService.unblockUser(user.getId(), userId);
        return Result.success();
    }
}
