package com.whpu.mybs.log.controller;

import com.whpu.mybs.common.constant.MqConstants;
import com.whpu.mybs.common.dto.R;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.log.dto.LogEntry;
import com.whpu.mybs.log.service.LogsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logs")
public class LogCollectController {

    private final RedisTemplate<String, String> redisTemplate;
    private final LogsService logsService;

    @PostMapping("/collect")
    public R<String> collect(@RequestBody LogEntry logEntry,
                                          @RequestHeader("X-API-Key") String apiKey) {
        // 1. 校验 API Key（Redis）
        Boolean isValid = redisTemplate.hasKey("api_key:" + apiKey);
        if (!isValid) {
            return R.fail(ResultCode.PARAM_MISSING, "Invalid API Key");
        }

        logsService.collect(logEntry);

        // 4. 立即返回成功
        return R.ok("Log accepted");
    }
}