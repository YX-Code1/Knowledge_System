package com.knowledge.base.service;

import com.knowledge.base.dto.LoginRequest;
import com.knowledge.base.dto.RegisterRequest;
import com.knowledge.base.dto.ResetPasswordRequest;
import com.knowledge.base.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginRequest request);
    void register(RegisterRequest request);
    void sendCode(String email);
    void sendResetCode(String username, String email);
    void resetPassword(ResetPasswordRequest request);
}
