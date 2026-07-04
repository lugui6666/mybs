package com.whpu.mybs.common.config;

import com.whpu.mybs.common.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器 — 从请求头中读取 Gateway 传递的用户信息
 */
@Slf4j
public class UserContextInterceptor implements HandlerInterceptor {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-User-Name";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String userId = request.getHeader(HEADER_USER_ID);
        String username = request.getHeader(HEADER_USERNAME);

        if (userId != null) {
            UserContext.setUserId(Long.valueOf(userId));
        }
        if (username != null) {
            UserContext.setUsername(username);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

}
