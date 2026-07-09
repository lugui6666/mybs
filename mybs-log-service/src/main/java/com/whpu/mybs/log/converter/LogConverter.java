package com.whpu.mybs.log.converter;

import com.whpu.mybs.log.dto.LogEntry;
import com.whpu.mybs.log.entity.AttackLog;

import java.time.Instant;
import java.util.UUID;

public class LogConverter {

    public static AttackLog toAttackLog(LogEntry entry) {
        return AttackLog.builder()
                .id(UUID.randomUUID().toString())      // 生成唯一ID
                .instanceId(entry.getInstanceId())
                .instanceName(entry.getInstanceName())
                .serviceType(entry.getServiceType())
                .logLevel(entry.getLogLevel())
                .sourceIp(entry.getSrcIp())
                .sourcePort(entry.getSrcPort())
                .sourceHostname(entry.getSrcHostname())
                .destinationPort(entry.getDestPort())
                .attackType(entry.getAttackType())
                .attackPayload(entry.getAttackPayload())
                .rawData(entry.getRawMessage())
                .geoIp(entry.getGeoIp())
                .attackTime(entry.getTimestamp() != null ? entry.getTimestamp() : Instant.now())
                .severity(entry.getSeverity())
                .build();
    }
}