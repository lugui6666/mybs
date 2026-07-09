package com.whpu.mybs.log.worker;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.whpu.mybs.common.constant.MqConstants;
import com.whpu.mybs.log.converter.LogConverter;
import com.whpu.mybs.log.dto.LogEntry;
import com.whpu.mybs.log.entity.AttackLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogWorker {

    private final ElasticsearchClient esClient;

    // 本地缓冲区（线程安全）
    private final List<AttackLog> buffer = new ArrayList<>();
    private static final int BATCH_SIZE = 1000;          // 每1000条批量写入
    private static final int FLUSH_INTERVAL_SECONDS = 5; // 每5秒强制刷新

    // 定时任务线程池
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // ========== 启动定时任务 ==========
    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::flushToES,
                FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("LogWorker 定时刷新任务已启动，间隔 {} 秒", FLUSH_INTERVAL_SECONDS);
    }

    // ========== 销毁时关闭线程池 ==========
    @PreDestroy
    public void destroy() {
        // 先强制刷新一次，把剩余数据写入ES
        flushToES();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("LogWorker 已关闭");
    }

    // ========== 监听队列，接收消息 ==========
    @RabbitListener(queues = MqConstants.LOG_QUEUE)
    public void handleLog(LogEntry entry) {
        // 转换 LogEntry -> AttackLog
        AttackLog doc = LogConverter.toAttackLog(entry);

        synchronized (buffer) {
            buffer.add(doc);
            if (buffer.size() >= BATCH_SIZE) {
                flushToES();  // 达到阈值立即写入
            }
        }
    }

    // ========== 批量写入 Elasticsearch ==========
    private void flushToES() {
        try {
            List<AttackLog> batch;
            synchronized (buffer) {
                if (buffer.isEmpty()) {
                    return;
                }
                batch = new ArrayList<>(buffer);
                buffer.clear();
            }

            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            String indexName = "honeypot-logs-" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

            for (AttackLog log : batch) {
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(log.getId())      // 这里如果 getId() 为 null，会抛异常，现在会被捕获
                                .document(log)
                        )
                );
            }

            // 原本的 try-catch 可以保留，也可以合并，但为了保险，建议直接合并掉
            BulkResponse response = esClient.bulk(bulkBuilder.build());
            if (response.errors()) {
                log.error("ES 批量写入存在错误，共 {} 条失败", response.items().size());
                response.items().forEach(item -> {
                    if (item.error() != null) {
                        log.error("失败文档 ID: {}, 错误: {}", item.id(), item.error().reason());
                    }
                });
            } else {
                log.info("成功写入 {} 条日志到索引 {}", batch.size(), indexName);
            }

        } catch (Exception e) {
            // 2. 这里会捕获所有异常（包括 IO 异常、空指针、序列化异常等）
            //    并打印出具体的堆栈信息，方便我们定位根因
            log.error("flushToES 执行发生异常，定时任务不会停止，将继续执行", e);

            // 【可选】如果你还想把数据放回缓冲区防止丢失，可以在这里加逻辑，
            // 但为了先定位问题，先打印堆栈，不恢复数据（防止死循环）
        }
    }
}

