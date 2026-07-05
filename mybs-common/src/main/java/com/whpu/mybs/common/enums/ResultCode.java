package com.whpu.mybs.common.enums;

import lombok.Getter;

/**
 * 统一返回码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // 参数校验
    PARAM_ERROR(400, "参数错误"),
    PARAM_MISSING(4001, "缺少必要参数"),

    // 认证授权
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无访问权限"),
    TOKEN_INVALID(4011, "Token 无效"),
    TOKEN_EXPIRED(4012, "Token 已过期"),
    LOGIN_FAILED(4015, "登录失败"),

    // 用户
    USER_NOT_FOUND(4041, "用户不存在"),
    USERNAME_EXISTS(4091, "用户名已存在"),
    USERNAME_OR_PASSWORD_ERROR(4013, "用户名或密码错误"),
    USER_DISABLED(4014, "用户已被禁用"),

    // 角色
    ROLE_NOT_FOUND(4042, "角色不存在"),
    ROLE_CODE_EXISTS(4092, "角色编码已存在"),

    // 蜜罐类型
    HP_TYPE_NOT_FOUND(4043, "蜜罐类型不存在"),
    HP_TYPE_CONFIG_ERROR(4093, "蜜罐类型配置不合法"),

    // 蜜罐实例
    HP_INSTANCE_NOT_FOUND(4044, "蜜罐实例不存在"),
    HP_INSTANCE_STATUS_ERROR(4002, "蜜罐实例状态异常，无法执行此操作"),

    // 日志
    LOG_NOT_FOUND(4045, "日志记录不存在"),

    // 系统
    SYSTEM_ERROR(5000, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
