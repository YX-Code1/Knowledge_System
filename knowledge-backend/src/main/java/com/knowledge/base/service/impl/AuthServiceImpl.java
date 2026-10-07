package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.dto.LoginRequest;
import com.knowledge.base.dto.RegisterRequest;
import com.knowledge.base.dto.ResetPasswordRequest;
import com.knowledge.base.entity.User;
import com.knowledge.base.mapper.UserMapper;
import com.knowledge.base.security.JwtUtils;
import com.knowledge.base.service.AuthService;
import com.knowledge.base.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    /** 注册验证码 Redis key 前缀 */
    private static final String CODE_KEY_PREFIX = "email:code:";
    /** 注册验证码发送冷却 Redis key 前缀（防止频繁发送） */
    private static final String CODE_COOLDOWN_PREFIX = "email:code:cooldown:";
    /** 重置密码验证码 Redis key 前缀 */
    private static final String RESET_CODE_KEY_PREFIX = "email:reset:code:";
    /** 重置密码验证码发送冷却 Redis key 前缀 */
    private static final String RESET_COOLDOWN_PREFIX = "email:reset:cooldown:";
    /** 验证码有效期（分钟） */
    private static final long CODE_EXPIRE_MINUTES = 5;
    /** 同一邮箱发送验证码间隔（秒） */
    private static final long CODE_SEND_INTERVAL_SECONDS = 60;
    /** 同一邮箱最多可注册的账号数 */
    private static final int MAX_ACCOUNTS_PER_EMAIL = 5;
    /** 账号与邮箱不匹配时的提示 */
    private static final String EMAIL_MISMATCH_MSG = "这个邮箱貌似不是您的呢喵~";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));

        if (user == null || user.getDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }

        if (Integer.valueOf(1).equals(user.getStatus())) {
            throw new RuntimeException("您的账号已被拉黑，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setAvatar(user.getAvatar());
        vo.setBalance(user.getBalance());
        return vo;
    }

    @Override
    public void register(RegisterRequest request) {
        User exist = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (exist != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 同一邮箱最多注册5个账号
        Long emailCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (emailCount != null && emailCount >= MAX_ACCOUNTS_PER_EMAIL) {
            throw new RuntimeException("该邮箱注册账号数量已达上限（最多5个）");
        }

        // 校验邮箱验证码
        String codeKey = CODE_KEY_PREFIX + request.getEmail();
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (savedCode == null) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!savedCode.equals(request.getCode())) {
            throw new RuntimeException("验证码错误");
        }
        // 验证通过后删除，防止重复使用
        stringRedisTemplate.delete(codeKey);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole("USER");
        userMapper.insert(user);
    }

    @Override
    public void sendCode(String email) {
        sendEmailCode(CODE_KEY_PREFIX + email, CODE_COOLDOWN_PREFIX + email, email, "注册");
    }

    @Override
    public void sendResetCode(String username, String email) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        // 校验账号绑定的邮箱与用户输入是否一致
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException(EMAIL_MISMATCH_MSG);
        }
        sendEmailCode(RESET_CODE_KEY_PREFIX + email, RESET_COOLDOWN_PREFIX + email, email, "重置密码");
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        // 再次校验邮箱归属，防止绕过
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new RuntimeException(EMAIL_MISMATCH_MSG);
        }

        String codeKey = RESET_CODE_KEY_PREFIX + request.getEmail();
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (savedCode == null) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!savedCode.equals(request.getCode())) {
            throw new RuntimeException("验证码错误");
        }
        // 验证通过后删除，防止重复使用
        stringRedisTemplate.delete(codeKey);

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.updateById(user);
    }

    /**
     * 发送邮箱验证码（注册与重置密码共用）
     */
    private void sendEmailCode(String codeKey, String cooldownKey, String email, String purpose) {
        // 60秒内同一邮箱不能重复发送
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
            throw new RuntimeException("发送太频繁，请60秒后再试");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", "知识库系统", mailFrom));
            message.setTo(email);
            message.setSubject("【知识库系统】" + purpose + "验证码");
            message.setText("您好！\n\n您的" + purpose + "验证码是：" + code + "\n验证码5分钟内有效，请勿泄露给他人。\n\n如非本人操作，请忽略本邮件。");
            mailSender.send(message);
        } catch (Exception e) {
            // 发送失败则清除验证码，避免用户拿到验证码却收不到邮件
            stringRedisTemplate.delete(codeKey);
            throw new RuntimeException("验证码邮件发送失败，请稍后再试");
        }

        // 发送成功后才记录冷却时间
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", CODE_SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
}
