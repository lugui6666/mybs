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

    /** 类型UUID */
    private String typeUuid;

    /** 类型名称 (如 "SSH", "HTTP") */
    private String typeName;

    /** 镜像名称 (如 "mysql_26.7.1") */
    private String imageName;

    /** 镜像ID */
    private String imageId;

    /** 配置信息 */
    private String config;

    /** 描述 */
    private String description;

    /** 最小cpu核数 */
    private Integer minCpu;

    /** 最小内存大小 */
    private Integer minMemory;

    /** 最小磁盘大小 */
    private Integer minDisk;

}
