package com.whpu.mybs.log.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whpu.mybs.common.dto.PageResult;
import com.whpu.mybs.common.dto.R;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.log.entity.AttackLog;
import com.whpu.mybs.log.service.AttackLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 攻击日志管理控制器
 */
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class AttackLogController {

    private final AttackLogService logService;

    /**
     * 分页查询攻击日志
     */
    @GetMapping("/page")
    public R<PageResult<AttackLog>> page(@RequestParam(defaultValue = "1") Long page,
                                          @RequestParam(defaultValue = "10") Long size,
                                          @RequestParam(required = false) Long instanceId,
                                          @RequestParam(required = false) String attackerIp,
                                          @RequestParam(required = false) String attackType,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                          @RequestParam(required = false) String keyword) {
        Page<AttackLog> pageResult = logService.page(
                new Page<>(page, size), instanceId, attackerIp, attackType,
                startTime, endTime, keyword);
        return R.ok(PageResult.of(pageResult.getRecords(), pageResult.getTotal(), page, size));
    }

    /**
     * 查询日志详情
     */
    @GetMapping("/{id}")
    public R<AttackLog> getById(@PathVariable Long id) {
        AttackLog log = logService.getById(id);
        if (log == null) {
            throw new BusinessException(ResultCode.LOG_NOT_FOUND);
        }
        return R.ok(log);
    }

    /**
     * 查询某实例的所有日志
     */
    @GetMapping("/instance/{instanceId}")
    public R<List<AttackLog>> listByInstance(@PathVariable Long instanceId) {
        Page<AttackLog> pageResult = logService.page(
                new Page<>(1, 100), instanceId, null, null, null, null, null);
        return R.ok(pageResult.getRecords());
    }

    /**
     * 攻击统计 — 按攻击类型
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam(required = false)
                                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("byType", logService.statsByType(startTime, endTime));
        result.put("byIp", logService.statsByIp(10, startTime, endTime));
        return R.ok(result);
    }

    /**
     * 批量上报日志 (供蜜罐实例调用)
     */
    @PostMapping("/batch")
    public R<Void> batchReport(@RequestBody List<AttackLog> logs) {
        logService.batchSave(logs);
        return R.ok("日志上报成功");
    }

}
