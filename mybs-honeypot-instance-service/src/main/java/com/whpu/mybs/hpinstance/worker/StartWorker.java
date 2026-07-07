package com.whpu.mybs.hpinstance.worker;

import com.whpu.mybs.common.constant.MqConstants;
import com.whpu.mybs.common.enums.InstanceStatus;
import com.whpu.mybs.hpinstance.entity.HoneypotInstance;
import com.whpu.mybs.hpinstance.service.DockerDeployService;
import com.whpu.mybs.hpinstance.service.HoneypotInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartWorker {

    private final HoneypotInstanceService instanceService;
    private final DockerDeployService dockerDeployService;

    @RabbitListener(queues = MqConstants.START_QUEUE)
    public void handleStartMessage(Long instanceId) {
        log.info("接收到启动实例消息，instanceId: {}", instanceId);

        HoneypotInstance instance = instanceService.getById(instanceId);
        if (instance == null) {
            log.warn("实例 {} 不存在，忽略启动消息", instanceId);
            return;
        }

        // 幂等性检查：如果不是 STOPPED 或 ERROR，忽略（或根据业务允许从 ERROR 启动）
        String status = instance.getStatus();
        if (InstanceStatus.RUNNING.getCode().equals(status)) {
            log.warn("实例 {} 已在运行中，忽略启动消息", instanceId);
            return;
        }
        // 可选：允许从 ERROR 状态启动？
        if (!InstanceStatus.STOPPED.getCode().equals(status) && !InstanceStatus.ERROR.getCode().equals(status)) {
            log.warn("实例 {} 当前状态为 {}，不允许启动", instanceId, status);
            return;
        }

        try {
            // 启动已存在的虚拟机（不复刻镜像，直接启动）
            dockerDeployService.startRealInstance(instance);

            // 更新状态为 RUNNING
            instanceService.updateDeployOrStopSuccess(
                    instance.getId(),
                    instance.getIpAddress(),
                    instance.getPort(),
                    InstanceStatus.RUNNING.getCode()
            );
            log.info("实例 {} 启动成功", instanceId);
        } catch (Exception e) {
            log.error("启动实例 {} 失败", instanceId, e);
            instanceService.updateStatus(instanceId, InstanceStatus.ERROR.getCode());
        }
    }
}