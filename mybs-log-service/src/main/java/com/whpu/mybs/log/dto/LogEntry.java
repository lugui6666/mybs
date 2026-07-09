package com.whpu.mybs.log.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    // === 元信息 ===
    private String logId;
    private Instant timestamp;
    private String serviceType;
    private String logLevel;

    // === 蜜罐实例 ===
    private Long instanceId;
    private String instanceName;

    // === 来源 ===
    private String srcIp;
    private Integer srcPort;
    private String srcHostname;

    // === 目标 ===
    private Integer destPort;

    // === 攻击数据 ===
    private String attackPayload;
    private String attackType;
    private String severity;

    // === 原始数据 ===
    private String rawMessage;

    // === 地理位置 ===
    private Map<String, Object> geoIp;
}