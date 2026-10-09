package com.knowledge.base.security;

import com.knowledge.base.entity.User;
import com.knowledge.base.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token)) {
            try {
                // 只解析一次：userId / role 从 JWT 声明中取，
                // 不再像旧实现那样 validate + getUserId + getUsername 三次验签解析
                Claims claims = jwtUtils.parseToken(token);
                Long userId = claims.get("userId", Long.class);

                if (userId != null) {
                    // 角色 / 封禁状态不走 JWT 声明，而是查 Redis 缓存（未命中查库），
                    // 保证管理员改角色、拉黑用户后能快速生效；缓存变更时主动失效，见 UserServiceImpl
                    User user = loadAuthUser(userId);
                    if (user != null) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(user, null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                // 无效 token 不设置认证，由 SecurityConfig 的 401 入口统一处理
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * 加载用户认证信息：先读 Redis（命中则跳过数据库，大幅减少每请求查库），
     * 未命中查库并回填缓存。缓存的是部分字段（role/status/deleted），
     * 用作 principal 时其余字段为 null——当前代码只在 principal 上使用 id 与 role，
     * 其余信息（昵称、余额等）一律按需重新查库，避免读到过期数据。
     *
     * 为什么还留了查库路径而不是完全信任 JWT：拉黑用户、调整角色需要尽快生效，
     * 只靠 JWT 声明要等 token 过期。Redis 缓存 60 秒 TTL + 变更时主动删除，
     * 是「少查库」与「变更即时生效」之间的折中。
     *
     * Redis 不可用时降级为直接查库（fail-open）：缓存只是性能优化，
     * 不应因为缓存故障导致认证不可用。
     */
    private User loadAuthUser(Long userId) {
        String cached = null;
        try {
            cached = stringRedisTemplate.opsForValue().get(AuthCache.key(userId));
        } catch (Exception e) {
            log.warn("Redis 认证缓存不可用，降级为直接查库：{}", e.getMessage());
        }
        if (cached != null) {
            String[] parts = cached.split("\\|");
            if (parts.length == 3) {
                User user = new User();
                user.setId(userId);
                user.setRole(parts[0]);
                user.setStatus(Integer.valueOf(parts[1]));
                user.setDeleted(Integer.valueOf(parts[2]));
                // 黑名单 / 已注销用户直接拒绝认证
                if (user.getDeleted() == 0 && !Integer.valueOf(1).equals(user.getStatus())) {
                    return user;
                }
                return null;
            }
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        try {
            stringRedisTemplate.opsForValue().set(AuthCache.key(userId),
                    user.getRole() + "|" + user.getStatus() + "|" + user.getDeleted(),
                    AuthCache.TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 回填失败不影响本次认证，下次请求继续查库
            log.warn("Redis 认证缓存回填失败：{}", e.getMessage());
        }

        if (user.getDeleted() == 0 && !Integer.valueOf(1).equals(user.getStatus())) {
            return user;
        }
        return null;
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(header);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(prefix + " ")) {
            return bearerToken.substring(prefix.length() + 1);
        }
        return null;
    }
}
