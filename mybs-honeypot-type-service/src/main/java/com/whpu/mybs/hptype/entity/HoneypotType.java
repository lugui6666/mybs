package com.whpu.mybs.hptype.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.whpu.mybs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 蜜罐类型实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_honeypot_type")
public class HoneypotType extends BaseEntity {

    /** 类型名称 (如 "SSH 蜜罐") */
    private String typeName;

    /** 类型编码 (如 "SSH", "HTTP") */
    private String typeCode;

    /** 协议 (HTTP, SSH, MySQL, Redis, FTP, ...) */
    private String protocol;

    /** 默认端口 */
    private Integer defaultPort;

    /** 描述 */
    private String description;

    /** 图标 */
    private String icon;

    /** 是否启用: 1=启用, 0=禁用 */
    private Integer enabled;

}
