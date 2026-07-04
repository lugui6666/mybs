package com.whpu.mybs.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.common.utils.JwtTokenProvider;
import com.whpu.mybs.common.service.SessionService;
import com.whpu.mybs.common.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import com.whpu.mybs.user.dto.LoginRequest;
import com.whpu.mybs.user.dto.LoginResponse;
import com.whpu.mybs.user.dto.RegisterRequest;
import com.whpu.mybs.user.entity.User;
import com.whpu.mybs.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;

    /**
     * 注册
     */
    @Transactional
    public void register(RegisterRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        userMapper.insert(user);
    }

    /**
     * 登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成 Token
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername());

        long ttl = jwtTokenProvider.getRemainingSeconds(token);

        // 6. 存储到 Redis，覆盖旧 Token（强制单设备登录）
        try {
            sessionService.saveUserToken(user.getId().toString(), token, ttl);
        } catch (Exception e) {
            log.error("存储 Token 到 Redis 失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.LOGIN_FAILED, "登录失败，请重试");
        }


        return LoginResponse.builder()
                .token(token)
                .expiresIn(86400000L)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    public void logout() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        sessionService.deleteUserToken(userId.toString());
    }

}
