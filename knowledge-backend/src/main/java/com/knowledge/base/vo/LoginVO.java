package com.knowledge.base.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String role;
    private String avatar;
    private java.math.BigDecimal balance;
}
