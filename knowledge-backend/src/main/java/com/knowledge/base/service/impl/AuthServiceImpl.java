package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.dto.LoginRequest;
import com.knowledge.base.dto.RegisterRequest;
import com.knowledge.base.dto.ResetPasswordRequest;
import com.knowledge.base.entity.User;
import com.knowledge.base.mapper.UserMapper;
import com.knowledge.base.security.JwtUtils;
import com.knowledge.base.service.AuthService;
import com.knowledge.base.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
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

    /** 登录失败计数 Redis key 前缀（按用户名） */
    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";
    /** 连续登录失败达到该次数后锁定 */
    private static final int LOGIN_FAIL_MAX = 5;
    /** 登录失败计数有效期（分钟），锁定期间不能登录 */
    private static final long LOGIN_FAIL_EXPIRE_MINUTES = 15;

    /** 验证码校验错误计数前缀（注册 / 重置密码各自独立，按邮箱） */
    private static final String CODE_TRY_KEY_PREFIX = "email:code:try:";
    private static final String RESET_TRY_KEY_PREFIX = "email:reset:try:";
    /** 同一验证码最多允许输错次数，超过后验证码作废 */
    private static final int CODE_TRY_MAX = 5;

    /** 发送验证码按 IP 限流：同一 IP 每小时最多发送次数 */
    private static final String CODE_IP_KEY_PREFIX = "email:code:ip:";
    private static final int CODE_IP_MAX = 10;
    private static final long CODE_IP_EXPIRE_MINUTES = 60;

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
    public LoginVO login(LoginRequest request, String ip) {
        // 登录失败限流：按用户名计数，5 次失败锁 15 分钟，防止撞库爆破
        String failKey = LOGIN_FAIL_KEY_PREFIX + request.getUsername();
        String failCount = redisWithFallback(null,
                () -> stringRedisTemplate.opsForValue().get(failKey));
        if (failCount != null && Integer.parseInt(failCount) >= LOGIN_FAIL_MAX) {
            throw new BusinessException("失败次数过多，请15分钟后再试");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));

        // 用户不存在与密码错误统一提示，防止用户名枚举
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户名或密码错误");
        }

        if (Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException("您的账号已被拉黑，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 只有密码错误才计数（用户不存在不计数，避免攻击者借此锁死任意账号）。
            // Redis INCR 原子自增，并发下计数不丢；首次失败时设置过期时间
            Long fails = redisWithFallback(0L,
                    () -> stringRedisTemplate.opsForValue().increment(failKey));
            if (fails != null && fails == 1) {
                redisWithFallback(null,
                        () -> stringRedisTemplate.expire(failKey, LOGIN_FAIL_EXPIRE_MINUTES, TimeUnit.MINUTES));
            }
            throw new BusinessException("用户名或密码错误");
        }

        // 登录成功清除失败计数
        redisWithFallback(null, () -> stringRedisTemplate.delete(failKey));

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
            throw new BusinessException("用户名已存在");
        }

        // 同一邮箱最多注册5个账号
        Long emailCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (emailCount != null && emailCount >= MAX_ACCOUNTS_PER_EMAIL) {
            throw new BusinessException("该邮箱注册账号数量已达上限（最多5个）");
        }

        // 校验邮箱验证码（带错误次数限制，防爆破）
        verifyCodeWithAttemptLimit(CODE_KEY_PREFIX, CODE_TRY_KEY_PREFIX,
                request.getEmail(), request.getCode());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole("USER");
        userMapper.insert(user);
    }

    @Override
    public void sendCode(String email, String ip) {
        sendEmailCode(CODE_KEY_PREFIX + email, CODE_COOLDOWN_PREFIX + email, email, "注册", ip);
    }

    @Override
    public void sendResetCode(String username, String email, String ip) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        // 校验账号绑定的邮箱与用户输入是否一致
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email)) {
            throw new BusinessException(EMAIL_MISMATCH_MSG);
        }
        sendEmailCode(RESET_CODE_KEY_PREFIX + email, RESET_COOLDOWN_PREFIX + email, email, "重置密码", ip);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        // 再次校验邮箱归属，防止绕过
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new BusinessException(EMAIL_MISMATCH_MSG);
        }

        // 校验验证码（带错误次数限制，防爆破）
        verifyCodeWithAttemptLimit(RESET_CODE_KEY_PREFIX, RESET_TRY_KEY_PREFIX,
                request.getEmail(), request.getCode());

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.updateById(user);
    }

    /**
     * 校验邮箱验证码，并限制错误尝试次数：
     * 同一验证码最多输错 {@link #CODE_TRY_MAX} 次，超过后验证码直接作废，必须重新获取。
     * 验证通过后删除验证码与尝试计数，防止重复使用。
     *
     * 降级策略：验证码本身的读取 fail-closed（Redis 不可用时无法核验，拒绝通过）；
     * 尝试计数属于加固手段，失败时不影响验证码核验本身。
     */
    private void verifyCodeWithAttemptLimit(String codePrefix, String tryPrefix, String email, String code) {
        String codeKey = codePrefix + email;
        String tryKey = tryPrefix + email;

        String savedCode;
        try {
            savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        } catch (Exception e) {
            log.warn("Redis 不可用，验证码无法核验：{}", e.getMessage());
            throw new BusinessException("验证码服务暂不可用，请稍后再试");
        }
        if (savedCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }

        String tryCount = redisWithFallback(null,
                () -> stringRedisTemplate.opsForValue().get(tryKey));
        if (tryCount != null && Integer.parseInt(tryCount) >= CODE_TRY_MAX) {
            // 错误次数过多直接作废验证码
            redisWithFallback(null, () -> stringRedisTemplate.delete(codeKey));
            throw new BusinessException("验证码错误次数过多，请重新获取");
        }

        if (!savedCode.equals(code)) {
            // 计数与验证码剩余有效期对齐（每次失败后重新续期，最坏情况略长于验证码有效期，可接受）
            redisWithFallback(null,
                    () -> stringRedisTemplate.opsForValue().increment(tryKey));
            redisWithFallback(null,
                    () -> stringRedisTemplate.expire(tryKey, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES));
            throw new BusinessException("验证码错误");
        }

        redisWithFallback(null, () -> stringRedisTemplate.delete(codeKey));
        redisWithFallback(null, () -> stringRedisTemplate.delete(tryKey));
    }

    /**
     * Redis 操作降级包装：Redis 不可用时记录告警并返回兜底值（fail-open）。
     * 用于限流、计数等加固类操作——它们不可用不应阻塞登录/注册主流程。
     */
    private <T> T redisWithFallback(T fallback, Supplier<T> action) {
        try {
            return action.get();
        } catch (Exception e) {
            log.warn("Redis 操作失败，降级处理：{}", e.getMessage());
            return fallback;
        }
    }

    /**
     * 发送邮箱验证码（注册与重置密码共用）。
     * 两层防护：按 IP 限流（防轰炸任意邮箱）+ 按邮箱冷却（防轰炸同一邮箱）。
     */
    private void sendEmailCode(String codeKey, String cooldownKey, String email, String purpose, String ip) {
        // 按 IP 限流：先原子自增再判断，避免 check-then-act 竞态；
        // 超限的这一次请求也会计入计数（多算一次），影响可忽略。
        // Redis 不可用时 fail-open：跳过限流，允许发送（限流是加固手段）
        String ipKey = CODE_IP_KEY_PREFIX + ip;
        Long ipCount = redisWithFallback(1L,
                () -> stringRedisTemplate.opsForValue().increment(ipKey));
        if (ipCount != null && ipCount == 1) {
            redisWithFallback(null,
                    () -> stringRedisTemplate.expire(ipKey, CODE_IP_EXPIRE_MINUTES, TimeUnit.MINUTES));
        }
        if (ipCount != null && ipCount > CODE_IP_MAX) {
            throw new BusinessException("发送验证码过于频繁，请稍后再试");
        }

        // 同一邮箱 60 秒冷却：setIfAbsent 原子占位，并发请求只有一个能拿到冷却位，
        // 替代原先 hasKey + set 的 check-then-act（存在竞态窗口）
        Boolean acquired = redisWithFallback(true, () -> stringRedisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", CODE_SEND_INTERVAL_SECONDS, TimeUnit.SECONDS));
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException("发送太频繁，请60秒后再试");
        }

        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        // 验证码必须先落库再发邮件，落库失败则不发（fail-closed），否则用户收不到可用的验证码
        Boolean codeSaved = redisWithFallback(null, () -> {
            stringRedisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return true;
        });
        if (!Boolean.TRUE.equals(codeSaved)) {
            throw new BusinessException("验证码服务暂不可用，请稍后再试");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", "知识库系统", mailFrom));
            message.setTo(email);
            message.setSubject("【知识库系统】" + purpose + "验证码");
            message.setText("您好！\n\n您的" + purpose + "验证码是：" + code + "\n验证码5分钟内有效，请勿泄露给他人。\n\n如非本人操作，请忽略本邮件。");
            mailSender.send(message);
        } catch (Exception e) {
            // 发送失败则清除验证码与冷却位，允许用户立即重试
            redisWithFallback(null, () -> stringRedisTemplate.delete(codeKey));
            redisWithFallback(null, () -> stringRedisTemplate.delete(cooldownKey));
            throw new BusinessException("验证码邮件发送失败，请稍后再试");
        }
    }
}
