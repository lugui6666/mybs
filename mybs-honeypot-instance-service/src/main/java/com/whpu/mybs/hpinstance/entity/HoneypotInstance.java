package com.whpu.mybs.hpinstance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.whpu.mybs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 蜜罐实例实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_honeypot_instance")
public class HoneypotInstance extends BaseEntity {

    /** 实例名称 */
    private String instanceName;

    /** 关联的蜜罐类型 ID */
    private Long typeId;

    /** 部署 IP 地址 */
    private String ipAddress;

    /** ssh端口 */
    private Integer port;

    /** 状态: deploying/running/stopped/error/destroyed */
    private String status;

    /** 描述 */
    private String description;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeat;

    /** 创建者用户 ID */
    private Long createUserId;

}
