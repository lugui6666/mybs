package com.whpu.mybs.common.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 会话服务 — 管理用户 Token 在 Redis 中的存储
 * <p>
 * 使用阻塞式 StringRedisTemplate，与 WebMVC / WebFlux 均兼容。
 * WebFlux 侧（Gateway）通过 Mono.fromCallable() 包装调用。
 */
@Service
public class SessionService {

    private final StringRedisTemplate redisTemplate;
    private static final String TOKEN_KEY_PREFIX = "user:token:";

    public SessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 存储用户的当前 Token，覆盖旧 Token（强制单设备登录）
     *
     * @param userId     用户 ID
     * @param token      JWT 字符串
     * @param ttlSeconds 有效期（秒）
     */
    public void saveUserToken(String userId, String token, long ttlSeconds) {
        String key = TOKEN_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, token, Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 获取用户当前存储的 Token
     */
    public String getUserToken(String userId) {
        String key = TOKEN_KEY_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除用户 Token（登出时调用）
     */
    public void deleteUserToken(String userId) {
        String key = TOKEN_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    /**
     * 验证当前 Token 是否与 Redis 中存储的一致（单设备登录校验）
     *
     * @param userId       用户 ID
     * @param currentToken 当前请求携带的 Token
     * @return true 表示有效且一致，false 表示不一致（已被其他登录踢下线）
     */
    public boolean validateTokenForUser(String userId, String currentToken) {
        String storedToken = getUserToken(userId);
        // Redis 中无记录则视为有效（可能是 Redis 数据过期，降级放行）
        return storedToken == null || storedToken.equals(currentToken);
    }

}
