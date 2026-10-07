package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.dto.ProfileUpdateRequest;
import com.knowledge.base.entity.BalanceLog;
import com.knowledge.base.entity.User;
import com.knowledge.base.mapper.BalanceLogMapper;
import com.knowledge.base.mapper.UserMapper;
import com.knowledge.base.service.UserService;
import com.knowledge.base.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final List<String> MANAGEABLE_ROLES = List.of("USER", "ADMIN");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    @Override
    public Page<UserVO> getUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        // VO 无密码字段，转换后即天然脱敏
        return (Page<UserVO>) result.convert(UserVO::from);
    }

    @Override
    public void updateRole(Long id, String role) {
        if (!MANAGEABLE_ROLES.contains(role)) {
            throw new RuntimeException("只能设置 USER 或 ADMIN 角色");
        }
        User target = getUserForManage(id);
        target.setRole(role);
        userMapper.updateById(target);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("状态值不合法");
        }
        User target = getUserForManage(id);
        target.setStatus(status);
        userMapper.updateById(target);
    }

    @Override
    @Transactional
    public void adjustBalance(Long id, BigDecimal change, String remark) {
        User target = userMapper.selectById(id);
        if (target == null) {
            throw new RuntimeException("用户不存在");
        }
        if (change == null || change.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("调整金额不能为 0");
        }
        // 调整后余额不能为负
        if (target.getBalance() == null
                || target.getBalance().add(change).compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("调整后余额不能为负");
        }
        userMapper.addBalance(id, change);

        BigDecimal balanceAfter = userMapper.selectById(id).getBalance();

        BalanceLog log = new BalanceLog();
        log.setUserId(id);
        log.setChangeAmount(change);
        log.setBalanceAfter(balanceAfter);
        log.setType("ADMIN");
        log.setRemark(StringUtils.hasText(remark) ? remark : "管理员调整余额");
        balanceLogMapper.insert(log);
    }

    @Override
    public UserVO updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    /**
     * 获取可管理的用户，并做保护性校验：
     * 不能操作自己，也不能操作其他超级管理员
     */
    private User getUserForManage(Long id) {
        User target = userMapper.selectById(id);
        if (target == null) {
            throw new RuntimeException("用户不存在");
        }
        if ("SUPER_ADMIN".equals(target.getRole())) {
            throw new RuntimeException("不能操作超级管理员账号");
        }
        Long currentUserId = getCurrentUserId();
        if (target.getId().equals(currentUserId)) {
            throw new RuntimeException("不能操作自己的账号");
        }
        return target;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new RuntimeException("无法识别当前登录用户");
    }
}
