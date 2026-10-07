package com.knowledge.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.Result;
import com.knowledge.base.entity.User;
import com.knowledge.base.service.NotificationService;
import com.knowledge.base.vo.NotificationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public Result<Page<NotificationVO>> getMyNotifications(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            return Result.success(notificationService.getMyNotifications(page, size, user.getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            return Result.success(notificationService.getUnreadCount(user.getId()));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            notificationService.markRead(id, user.getId());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(@AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            notificationService.markAllRead(user.getId());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
