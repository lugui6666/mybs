package com.whpu.mybs.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.whpu.mybs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 攻击日志实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_attack_log")
public class AttackLog extends BaseEntity {

    /** 关联的蜜罐实例 ID */
    private Long instanceId;

    /** 实例名称 (冗余，方便查询) */
    private String instanceName;

    /** 攻击来源 IP */
    private String attackerIp;

    /** 攻击来源端口 */
    private Integer sourcePort;

    /** 攻击类型 (如 BRUTE_FORCE, CVE_EXPLOIT, SCAN 等) */
    private String attackType;

    /** 攻击载荷 (请求内容) */
    private String attackPayload;

    /** 原始数据 (完整日志) */
    private String rawData;

    /** 地理位置信息 */
    private String geoLocation;

    /** 攻击时间 */
    private LocalDateTime attackTime;

}
