package com.knowledge.base.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：负责 Token 的生成与解析。
 *
 * 注意：没有做成纯静态工具类，而是注册为 Spring 组件（@Component），
 * 因为 secret / expiration 来自配置文件，需要依赖注入（@Value 无法注入静态字段）。
 */
@Component
public class JwtUtils {

    /** 签名密钥（明文），来自配置 jwt.secret */
    @Value("${jwt.secret}")
    private String secret;

    /** Token 有效期，单位毫秒，来自配置 jwt.expiration */
    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * 把配置里的明文字符串转成 HMAC-SHA256 签名所需的密钥对象。
     * 要求 secret 至少 32 字节（256 位），否则会抛 WeakKeyException。
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Token：
     * subject 存用户名，userId / role 为自定义声明；
     * issuedAt / expiration 分别是签发时间和过期时间；
     * 最后用密钥签名并压缩为字符串返回。
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 Token：验签 + 校验过期时间，返回载荷（Claims）。
     * Token 无效（签名被篡改、已过期、格式错误等）会抛 JwtException，由调用方处理。
     * 调用方只需解析一次，从返回的 Claims 中按需取 userId / role 等声明。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
