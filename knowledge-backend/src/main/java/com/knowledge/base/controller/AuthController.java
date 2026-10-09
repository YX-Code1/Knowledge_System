package com.knowledge.base.controller;

import com.knowledge.base.common.Result;
import com.knowledge.base.dto.LoginRequest;
import com.knowledge.base.dto.RegisterRequest;
import com.knowledge.base.dto.ResetCodeRequest;
import com.knowledge.base.dto.ResetPasswordRequest;
import com.knowledge.base.dto.SendCodeRequest;
import com.knowledge.base.service.AuthService;
import com.knowledge.base.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, getClientIp(httpRequest)));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request, HttpServletRequest httpRequest) {
        authService.sendCode(request.getEmail(), getClientIp(httpRequest));
        return Result.success("验证码已发送", null);
    }

    @PostMapping("/reset-code")
    public Result<Void> sendResetCode(@Valid @RequestBody ResetCodeRequest request, HttpServletRequest httpRequest) {
        authService.sendResetCode(request.getUsername(), request.getEmail(), getClientIp(httpRequest));
        return Result.success("验证码已发送", null);
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.success();
    }

    /**
     * 获取客户端 IP，用于登录失败与验证码发送的限流。
     * 直接取 remoteAddr（本地开发经 Vite 代理访问时均为 127.0.0.1，限流按全局生效，无碍）。
     * 部署在反向代理后时需改取 X-Forwarded-For 并注意其可伪造性。
     */
    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
