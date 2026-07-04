package com.whpu.mybs.gateway.filter;

import com.whpu.mybs.common.service.SessionService;
import com.whpu.mybs.common.utils.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 全局 JWT 认证过滤器
 * <p>
 * 在 Gateway 层统一校验 JWT Token，将用户信息通过请求头传给下游微服务。
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private final List<String> whiteList;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private final SessionService sessionService;

    public AuthGlobalFilter(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long expiration, SessionService sessionService) {
        this.sessionService = sessionService;
        this.jwtTokenProvider = new JwtTokenProvider(secret, expiration);
        this.whiteList = Arrays.asList(
                "/api/auth/login",
                "/api/auth/register"
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单路径跳过鉴权
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 获取 Token
        String token = extractToken(request);
        if (token == null) {
            return unauthorized(exchange, "未提供认证令牌");
        }

        // 校验 Token
        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(token);
        } catch (Exception e) {
            log.warn("解析 Token 异常: {}", e.getMessage());
            return unauthorized(exchange, "认证令牌格式错误");
        }

        String userId = String.valueOf(claims.get("userId"));

        // 2. 查询 Redis 中该用户的最新 Token 并对比（用 Mono.fromCallable 包装阻塞调用）
        return Mono.fromCallable(() -> sessionService.validateTokenForUser(userId, token))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(isValid -> {
                    if (!isValid) {
                        log.info("Token 不匹配，可能已在其他设备登录: userId={}", userId);
                        return unauthorized(exchange, "账号已在其他设备登录，请重新登录");
                    }

                    // 3. 存入 exchange 属性，供后续（如登出接口）使用
                    exchange.getAttributes().put("token", token);
                    exchange.getAttributes().put("userId", userId);

                    // 4. 添加用户信息到请求头，传递给下游
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Name", String.valueOf(claims.get("username")))
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(e -> {
                    log.error("Redis 校验异常: {}", e.getMessage(), e);
                    // 降级：Redis 故障时，为保障可用性，可放行（也可拒绝，根据业务决定）
                    log.warn("Redis 不可用，暂时放行请求");
                    exchange.getAttributes().put("token", token);
                    exchange.getAttributes().put("userId", userId);
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Name", String.valueOf(claims.get("username")))
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    private boolean isWhiteListed(String path) {
        return whiteList.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                message, System.currentTimeMillis());
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级
    }

}
