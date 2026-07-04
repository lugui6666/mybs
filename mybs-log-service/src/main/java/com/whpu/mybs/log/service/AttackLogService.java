package com.whpu.mybs.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.log.entity.AttackLog;
import com.whpu.mybs.log.mapper.AttackLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 攻击日志服务
 */
@Service
@RequiredArgsConstructor
public class AttackLogService extends ServiceImpl<AttackLogMapper, AttackLog> {

    private final AttackLogMapper logMapper;

    /**
     * 分页查询 (支持多条件筛选)
     */
    public Page<AttackLog> page(Page<AttackLog> page, Long instanceId, String attackerIp,
                                 String attackType, LocalDateTime startTime,
                                 LocalDateTime endTime, String keyword) {
        LambdaQueryWrapper<AttackLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(instanceId != null, AttackLog::getInstanceId, instanceId)
                .eq(StringUtils.hasText(attackerIp), AttackLog::getAttackerIp, attackerIp)
                .eq(StringUtils.hasText(attackType), AttackLog::getAttackType, attackType)
                .ge(startTime != null, AttackLog::getAttackTime, startTime)
                .le(endTime != null, AttackLog::getAttackTime, endTime)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(AttackLog::getAttackPayload, keyword)
                        .or()
                        .like(AttackLog::getRawData, keyword))
                .orderByDesc(AttackLog::getAttackTime);
        return logMapper.selectPage(page, wrapper);
    }

    /**
     * 批量保存日志 (供蜜罐实例回传)
     */
    public void batchSave(List<AttackLog> logs) {
        if (logs != null && !logs.isEmpty()) {
            saveBatch(logs, 100);
        }
    }

    /**
     * 攻击统计 — 按攻击类型
     */
    public List<Map<String, Object>> statsByType(LocalDateTime startTime, LocalDateTime endTime) {
        // 简化实现：查询所有类型并分组计数
        LambdaQueryWrapper<AttackLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AttackLog::getAttackType)
                .ge(startTime != null, AttackLog::getAttackTime, startTime)
                .le(endTime != null, AttackLog::getAttackTime, endTime);
        List<AttackLog> logs = logMapper.selectList(wrapper);

        Map<String, Long> typeCount = new LinkedHashMap<>();
        for (AttackLog log : logs) {
            String type = StringUtils.hasText(log.getAttackType()) ? log.getAttackType() : "UNKNOWN";
            typeCount.merge(type, 1L, Long::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        typeCount.forEach((type, count) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("attackType", type);
            item.put("count", count);
            result.add(item);
        });
        return result;
    }

    /**
     * 攻击统计 — 按 IP (Top N)
     */
    public List<Map<String, Object>> statsByIp(int topN, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AttackLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AttackLog::getAttackerIp)
                .ge(startTime != null, AttackLog::getAttackTime, startTime)
                .le(endTime != null, AttackLog::getAttackTime, endTime);
        List<AttackLog> logs = logMapper.selectList(wrapper);

        Map<String, Long> ipCount = new LinkedHashMap<>();
        for (AttackLog log : logs) {
            String ip = StringUtils.hasText(log.getAttackerIp()) ? log.getAttackerIp() : "UNKNOWN";
            ipCount.merge(ip, 1L, Long::sum);
        }

        return ipCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("attackerIp", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .toList();
    }

}
