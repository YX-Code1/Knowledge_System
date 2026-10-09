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
        return Result.success(userService.getUsers(page, size, keyword));
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        userService.updateRole(id, request.getRole());
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        userService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @PutMapping("/{id}/balance")
    public Result<Void> adjustBalance(@PathVariable Long id, @Valid @RequestBody AdjustBalanceRequest request) {
        userService.adjustBalance(id, request.getChange(), request.getRemark());
        return Result.success();
    }
}
