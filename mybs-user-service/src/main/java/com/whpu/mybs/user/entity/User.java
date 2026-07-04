package com.whpu.mybs.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.whpu.mybs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user")
public class User extends BaseEntity {

    /** 用户名 */
    private String username;

    /** 密码 (BCrypt 加密) */
    private String password;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;


    /** 最后登录时间 */
    private java.time.LocalDateTime lastLoginTime;

}
