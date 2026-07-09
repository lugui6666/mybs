package com.whpu.mybs.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 攻击日志 - Elasticsearch 文档实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttackLog {

    // ===== ES 文档 ID（对应 ES 的 _id） =====
    @JsonProperty("id")
    private String id;

    // ===== 蜜罐实例 =====
    private Long instanceId;
    private String instanceName;

    // ===== 服务信息 =====
    private String serviceType;      // MYSQL, SSH, NGINX
    private String logLevel;         // INFO, WARN, ERROR

    // ===== 攻击来源 =====
    private String sourceIp;         // ES 会映射为 ip 类型
    private Integer sourcePort;
    private String sourceHostname;

    // ===== 攻击目标（蜜罐自身） =====
    private Integer destinationPort; // 建议加上，比如 3306

    // ===== 攻击载荷与分析 =====
    private String attackType;       // SQL_INJECTION, BRUTE_FORCE
    private String attackPayload;    // 长文本，全文检索
    private String rawData;          // 完整原始日志（兜底）

    // ===== 地理位置（Filebeat 可以自动填充） =====
    private Map<String, Object> geoIp;

    // ===== 攻击时间（使用 Instant，ES 自动识别为 date） =====
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant attackTime;

    // ===== 可选：威胁等级（后续扩展） =====
    private String severity;         // CRITICAL, HIGH, MEDIUM, LOW
}