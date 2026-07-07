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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StopWorker {

    private final HoneypotInstanceService instanceService;
    private final DockerDeployService dockerDeployService;

    @RabbitListener(queues = MqConstants.STOP_QUEUE)
    public void handleStopMessage(Long instanceId) {
        log.info("接收到停止实例消息，instanceId: {}", instanceId);

        // 1. 查询实例
        HoneypotInstance instance = instanceService.getById(instanceId);
        if (instance == null) {
            log.warn("实例 {} 不存在，忽略停止消息", instanceId);
            return;
        }

        // 2. 幂等性检查：如果不是 RUNNING 状态，直接忽略
        if (!InstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            log.warn("实例 {} 当前状态为 {}，不是 RUNNING，忽略停止消息", instanceId, instance.getStatus());
            return;
        }

        try {
            // 3. 执行真实的停止操作（外部资源）
            dockerDeployService.stopVm(instance);

            // 4. 更新状态为 STOPPED（独立事务）
            instanceService.updateDeployOrStopSuccess(
                    instance.getId(),
                    null,
                    null,
                    InstanceStatus.STOPPED.getCode()
            );
            log.info("实例 {} 停止成功", instanceId);

        } catch (Exception e) {
            log.error("停止实例 {} 失败", instanceId, e);
            // 5. 更新状态为 ERROR
            instanceService.updateStatus(instanceId, InstanceStatus.ERROR.getCode());
            // 注意：这里不抛出异常，消息会被正常确认，避免无限重试
        }
    }
}