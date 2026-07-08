package com.whpu.mybs.hpinstance.worker;


import com.whpu.mybs.common.constant.MqConstants;
import com.whpu.mybs.common.enums.InstanceStatus;
import com.whpu.mybs.hpinstance.entity.HoneypotInstance;
import com.whpu.mybs.hpinstance.mapper.HoneypotInstanceMapper;
import com.whpu.mybs.hpinstance.service.DockerDeployService;
import com.whpu.mybs.hpinstance.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckWorker {
    private final HoneypotInstanceMapper instanceMapper;
    private final DockerDeployService dockerDeployService;
    private final RabbitTemplate rabbitTemplate;
    private final HealthCheckService healthCheckService;

    /**
     * 消费健康检查消息（工作队列）
     */
    @RabbitListener(queues = MqConstants.HEALTH_WORK_QUEUE)
    public void handleHealthCheck(Long instanceId) {
        log.debug("收到健康检查消息，实例 ID: {}", instanceId);

        // 1. 查询实例
        HoneypotInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            log.warn("实例 {} 不存在，停止健康检查", instanceId);
            return;
        }

        // 2. 只对 RUNNING 状态的实例进行检查
        if (!InstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            log.info("实例 {} 状态不是 RUNNING（当前 {}），停止健康检查", instanceId, instance.getStatus());
            return;
        }

        // 3. 执行容器健康检查
        boolean healthy = checkContainersInVm(instance);

        if(healthy){
            // 健康：更新心跳，并安排下一次检查（30-40 秒后）
            instance.setLastHeartbeat(LocalDateTime.now());
            instanceMapper.updateById(instance);
            log.debug("实例 {} 健康，安排下一次检查", instanceId);
            healthCheckService.sendHealthCheck(instanceId);
        } else if (!InstanceStatus.STOPPED.getCode().equals(instance.getStatus()) && !InstanceStatus.DESTROYED.getCode().equals(instance.getStatus())) {
            // 不健康：标记 ERROR
            log.warn("实例 {} 容器不健康，标记为 ERROR", instanceId);
            instance.setStatus(InstanceStatus.ERROR.getCode());
            instanceMapper.updateById(instance);
            healthCheckService.sendHealthCheck(instanceId);
        }
    }

    /**
     * 实际检查逻辑（复用 HealthCheckService 中的方法）
     */
    private boolean checkContainersInVm(HoneypotInstance instance) {
        // 这里可以直接调用之前写的 checkContainersInVm 方法，或者移到一个公共工具类
        // 建议将检查逻辑提取到独立的 HealthCheckService 中，这里调用
        return healthCheckService.checkContainersInVm(instance);
    }
}