package com.knowledge.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.Result;
import com.knowledge.base.dto.AdjustBalanceRequest;
import com.knowledge.base.dto.UpdateRoleRequest;
import com.knowledge.base.dto.UpdateStatusRequest;
import com.knowledge.base.service.UserService;
import com.knowledge.base.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<Page<UserVO>> getUsers(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String keyword) {
        try {
            return Result.success(userService.getUsers(page, size, keyword));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        try {
            userService.updateRole(id, request.getRole());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        try {
            userService.updateStatus(id, request.getStatus());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/balance")
    public Result<Void> adjustBalance(@PathVariable Long id, @Valid @RequestBody AdjustBalanceRequest request) {
        try {
            userService.adjustBalance(id, request.getChange(), request.getRemark());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
