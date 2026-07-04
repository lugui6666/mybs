package com.whpu.mybs.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT 令牌工具类 — 生成、解析、校验 Token
 */
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtTokenProvider(String secret, long expiration) {
        // 使用明文密钥的 UTF-8 字节创建 HMAC-SHA 密钥 (>256 bits required)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    /**
     * 生成 JWT Token
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return JWT 字符串
     */
    public String createToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 Token 中解析 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT Token 已过期");
        } catch (UnsupportedJwtException e) {
            log.debug("不支持的 JWT Token");
        } catch (MalformedJwtException e) {
            log.debug("JWT Token 格式错误");
        } catch (IllegalArgumentException e) {
            log.debug("JWT Token 为空");
        } catch (Exception e) {
            log.debug("JWT Token 校验失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 获取 Token 剩余秒数
     */
    public long getRemainingSeconds(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        long now = System.currentTimeMillis();
        long diff = expiration.getTime() - now;
        return diff > 0 ? diff / 1000 : 0;
    }
}
