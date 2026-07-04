package com.whpu.mybs.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT Token */
    private String token;

    /** Token 过期时间 (毫秒) */
    private Long expiresIn;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

}
