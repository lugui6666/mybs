package com.whpu.mybs.user.controller;

import com.whpu.mybs.common.dto.R;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.common.utils.UserContext;
import com.whpu.mybs.user.dto.LoginRequest;
import com.whpu.mybs.user.dto.LoginResponse;
import com.whpu.mybs.user.dto.RegisterRequest;
import com.whpu.mybs.user.entity.User;
import com.whpu.mybs.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 认证控制器 — 登录 / 注册 / 当前用户
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return R.ok("登录成功", response);
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return R.ok("注册成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public R<User> currentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 不返回密码
        user.setPassword(null);
        return R.ok(user);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        userService.logout();
        return R.ok("退出登录成功");
    }

}
