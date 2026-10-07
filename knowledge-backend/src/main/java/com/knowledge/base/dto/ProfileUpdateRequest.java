package com.knowledge.base.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(max = 50, message = "昵称最多 50 字")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最多 100 字")
    private String email;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;
}
