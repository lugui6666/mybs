package com.whpu.mybs.hpinstance.service;

import com.whpu.mybs.common.constant.MqConstants;
import com.whpu.mybs.hpinstance.entity.HoneypotInstance;
import com.whpu.mybs.hpinstance.utils.ExecuteWslUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;



@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {
    private final RabbitTemplate rabbitTemplate;

    private final ExecuteWslUtil executeWslUtil;

    @Value("${deploy.vm.ssh.password:123456}")
    private String vmPassword;

    @Value("${health.check.timeout:5}")
    private int checkTimeout; // SSH 命令超时（秒）

    /**
     * 立即发送健康检查消息（直接进入工作队列）
     */
    public void sendImmediateHealthCheck(Long instanceId) {
        rabbitTemplate.convertAndSend(
                MqConstants.HEALTH_EXCHANGE,
                MqConstants.HEALTH_WORK_ROUTING_KEY,
                instanceId,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                }
        );
        log.info("发送立即健康检查消息，实例 ID: {}", instanceId);
    }

    public void sendHealthCheck(Long instanceId) {
        int expire = 30000 + (int) (Math.random() * 10000);
        // 发送到延迟队列，TTL 为 30-40 秒
        rabbitTemplate.convertAndSend(
                MqConstants.HEALTH_EXCHANGE,
                MqConstants.HEALTH_DELAY_ROUTING_KEY,
                instanceId,
                message -> {
                    message.getMessageProperties().setExpiration(Integer.toString(expire));
                    return message;
                }
        );
        log.info("发送健康检查消息，实例 ID: {}", instanceId);
    }

    /**
     * 通过 SSH 检查虚拟机内是否有容器在运行
     */
    public boolean checkContainersInVm(HoneypotInstance instance) {
        Long id = instance.getId();
        Integer sshPort = instance.getPort();
        if (sshPort == null) {
            log.warn("实例 {} 缺少 ssh_port，无法检查", id);
            return false;
        }

        try {
            // 执行 docker ps 获取容器名称列表
            String cmd = String.format(
                    "sshpass -p '%s' ssh -o StrictHostKeyChecking=no -o ConnectTimeout=%d -p %d root@localhost " +
                            "'docker ps --format \"{{.Names}}\" 2>/dev/null | head -1'",
                    vmPassword, checkTimeout, sshPort
            );
            String result = executeWslUtil.executeWsl(cmd);
            // 如果结果非空，说明有容器在运行
            if (!result.trim().isEmpty()) {
                return true;
            }
            log.warn("实例 {} 无运行中的容器", id);
        } catch (Exception e) {
            log.warn("检查实例 {} 容器失败: {}", id, e.getMessage());
        }
        return false;
    }
}