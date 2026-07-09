package com.whpu.mybs.log.service;

import com.whpu.mybs.common.constant.MqConstants;
import com.whpu.mybs.log.dto.LogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogsService {
    private final RabbitTemplate rabbitTemplate;

    public void collect(LogEntry logEntry) {
        // 2. 补充缺失字段（ID、时间戳）
        if (logEntry.getLogId() == null) {
            logEntry.setLogId(UUID.randomUUID().toString());
        }
        if (logEntry.getTimestamp() == null) {
            logEntry.setTimestamp(Instant.now());
        }

        // 3. 发送到 RabbitMQ（自动序列化为 JSON）
        rabbitTemplate.convertAndSend(
                MqConstants.LOG_EXCHANGE,
                MqConstants.LOG_ROUTING_KEY,
                logEntry
        );
        log.info("已发送消息到队列");
    }
}
