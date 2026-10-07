package com.knowledge.base.vo;

import com.knowledge.base.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户信息（不含密码等敏感字段）
 */
@Data
public class UserVO {
    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String avatar;

    /** 账户余额 */
    private BigDecimal balance;

    private String role;

    /** 状态：0-正常 1-黑名单 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setBalance(user.getBalance());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
