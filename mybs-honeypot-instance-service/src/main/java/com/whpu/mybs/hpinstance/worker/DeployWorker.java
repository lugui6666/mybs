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

// 2. 独立的部署 Worker（消费者）
@Slf4j
@Component
@RequiredArgsConstructor
public class DeployWorker {
    private final HoneypotInstanceService instanceService;
    private final DockerDeployService dockerDeployService;

    @RabbitListener(queues = MqConstants.DEPLOY_QUEUE)
    public void handleDeployMessage(Long instanceId) {
        // 1. 根据 ID 查询实例详情
        HoneypotInstance instance = instanceService.getById(instanceId);

        // 跳过重复部署
        if (!InstanceStatus.DEPLOYING.getCode().equals(instance.getStatus())) {
            log.warn("实例 {} 状态不是DEPLOYING，跳过重复部署", instanceId);
            return;
        }

        // 2. 执行真正的部署逻辑（SSH、Docker、QEMU等）
        // 注意：这里不再需要 @Transactional，因为外部资源操作无法被数据库事务回滚
        try {
            // 调用你之前提到的 DockerDeployService 执行真实部署
            dockerDeployService.startRealInstance(instance);

            // 3. 部署成功，更新数据库状态为 "RUNNING"
            instanceService.updateStatus(instance.getId(), InstanceStatus.RUNNING.getCode());
        } catch (Exception e) {
            // 4. 部署失败，更新状态为 "ERROR"，并执行补偿清理
            log.error("部署失败，instanceId: {}", instanceId, e);
            dockerDeployService.cleanupFailedInstance(instance);
            instanceService.updateStatus(instance.getId(), InstanceStatus.ERROR.getCode());
        }
    }
}