package com.knowledge.base.controller;

import com.knowledge.base.common.Result;
import com.knowledge.base.dto.LoginRequest;
import com.knowledge.base.dto.RegisterRequest;
import com.knowledge.base.dto.ResetCodeRequest;
import com.knowledge.base.dto.ResetPasswordRequest;
import com.knowledge.base.dto.SendCodeRequest;
import com.knowledge.base.service.AuthService;
import com.knowledge.base.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginVO vo = authService.login(request);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        try {
            authService.sendCode(request.getEmail());
            return Result.success("验证码已发送", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/reset-code")
    public Result<Void> sendResetCode(@Valid @RequestBody ResetCodeRequest request) {
        try {
            authService.sendResetCode(request.getUsername(), request.getEmail());
            return Result.success("验证码已发送", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
