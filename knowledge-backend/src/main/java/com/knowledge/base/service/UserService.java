package com.knowledge.base.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.dto.ProfileUpdateRequest;
import com.knowledge.base.vo.UserVO;

import java.math.BigDecimal;

public interface UserService {
    Page<UserVO> getUsers(int page, int size, String keyword);
    void updateRole(Long id, String role);
    void updateStatus(Long id, Integer status);
    /** 超级管理员调整任意用户余额（含自己），记 ADMIN 流水 */
    void adjustBalance(Long id, BigDecimal change, String remark);
    /** 修改自己的资料（昵称/邮箱/头像），返回更新后的用户（不含密码） */
    UserVO updateProfile(Long userId, ProfileUpdateRequest request);
}
