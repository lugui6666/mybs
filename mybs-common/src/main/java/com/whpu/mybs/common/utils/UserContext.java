package com.whpu.mybs.common.utils;

/**
 * 用户上下文 — 通过 ThreadLocal 保存当前请求的用户信息
 * <p>
 * 在 Gateway 的 JWT 过滤器中解析 Token 后，
 * 将 userId/username 写入请求头 X-User-Id / X-User-Name 传给下游服务。
 * 下游服务通过拦截器从请求头读取并设置到此类中。
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void setUsername(String username) {
        USERNAME_HOLDER.set(username);
    }

    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
    }

}
