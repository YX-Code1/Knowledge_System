package com.knowledge.base.controller;

import com.knowledge.base.common.Result;
import com.knowledge.base.dto.ProfileUpdateRequest;
import com.knowledge.base.entity.User;
import com.knowledge.base.service.UserService;
import com.knowledge.base.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 个人资料（自己的账号信息，任意登录用户可用）
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    /** 获取自己的资料 */
    @GetMapping
    public Result<UserVO> getProfile(@AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            // VO 无密码字段，转换后即天然脱敏（也不再就地修改 principal）
            return Result.success(UserVO.from(user));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 修改自己的资料（昵称/邮箱/头像） */
    @PutMapping
    public Result<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                      @AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return Result.error("请先登录");
            }
            return Result.success(userService.updateProfile(user.getId(), request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
