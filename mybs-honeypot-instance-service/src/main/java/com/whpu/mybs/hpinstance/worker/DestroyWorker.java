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
public class DestroyWorker {

    private final HoneypotInstanceService instanceService;
    private final DockerDeployService dockerDeployService;

    @RabbitListener(queues = MqConstants.DESTROY_QUEUE)
    public void handleDestroyMessage(Long instanceId) {
        log.info("接收到销毁实例消息，instanceId: {}", instanceId);

        HoneypotInstance instance = instanceService.getById(instanceId);
        if (instance == null) {
            log.warn("实例 {} 不存在，忽略销毁消息", instanceId);
            return;
        }

        String status = instance.getStatus();
        if (InstanceStatus.DESTROYED.getCode().equals(status)) {
            log.warn("实例 {} 已销毁，忽略消息", instanceId);
            return;
        }

        try {
            // 1. 如果实例正在运行，先停止
            if (InstanceStatus.RUNNING.getCode().equals(status)) {
                log.info("实例 {} 正在运行，先停止", instanceId);
                dockerDeployService.stopVm(instance);
                // 停止后更新状态为 STOPPED（以便后续销毁）
                instanceService.updateStatus(instanceId, InstanceStatus.STOPPED.getCode());
                // 重新获取实例（或直接使用已有的，但状态已变，需刷新）
                instance = instanceService.getById(instanceId);
            }

            // 2. 销毁虚拟机资源（删除镜像、释放端口/IP等）
            dockerDeployService.destroyVm(instance);

            // 3. 更新数据库为 DESTROYED
            instanceService.updateDeployOrStopSuccess(
                    instance.getId(),
                    null,
                    null,
                    InstanceStatus.DESTROYED.getCode()
            );
            // 可选：清理 IP、端口等字段，但 DESTROYED 状态已表示不再使用
            instanceService.removeById(instanceId);
            log.info("实例 {} 销毁成功", instanceId);

        } catch (Exception e) {
            log.error("销毁实例 {} 失败", instanceId, e);
            instanceService.updateStatus(instanceId, InstanceStatus.ERROR.getCode());
        }
    }
}