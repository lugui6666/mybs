package com.whpu.mybs.hpinstance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whpu.mybs.common.constant.MqConstants;
import com.whpu.mybs.common.enums.InstanceStatus;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.common.utils.UserContext;
import com.whpu.mybs.hpinstance.dto.DeployHoneypotInstanceRequest;
import com.whpu.mybs.hpinstance.entity.HoneypotInstance;
import com.whpu.mybs.hpinstance.mapper.HoneypotInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 蜜罐实例服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HoneypotInstanceService extends ServiceImpl<HoneypotInstanceMapper, HoneypotInstance> {

    private final HoneypotInstanceMapper instanceMapper;

    private final RabbitTemplate rabbitTemplate;

    /**
     * 分页查询
     */
    public Page<HoneypotInstance> page(Page<HoneypotInstance> page, String keyword,
                                        String status, Long typeId) {
        LambdaQueryWrapper<HoneypotInstance> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(HoneypotInstance::getInstanceName, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(HoneypotInstance::getStatus, status);
        }
        if (typeId != null) {
            wrapper.eq(HoneypotInstance::getTypeId, typeId);
        }
        wrapper.orderByDesc(HoneypotInstance::getCreateTime);
        return instanceMapper.selectPage(page, wrapper);
    }

    /**
     * 创建/部署实例
     */
    @Transactional
    public void deploy(DeployHoneypotInstanceRequest request) {
        HoneypotInstance instance = new HoneypotInstance();
        // 1. 数据库插入，状态为"DEPLOYING"
        instance.setCreateUserId(UserContext.getUserId());
        instance.setStatus(InstanceStatus.DEPLOYING.getCode());
        instance.setInstanceName(request.getInstanceName());
        instance.setTypeId(request.getTypeId());
        instance.setDescription(request.getDescription());
        instanceMapper.insert(instance);

        // 2. 发送部署消息到 RabbitMQ
        // 消息体可以只包含 instance ID，消费者再根据ID查询详情
        rabbitTemplate.convertAndSend(MqConstants.DEPLOY_EXCHANGE, MqConstants.DEPLOY_ROUTING_KEY, instance.getId());

        // 3. 事务提交，数据库状态变更和消息发送（大致）同时成功
        // 注意：这里为确保可靠性，需配置事务同步，确保消息在事务提交后才真正发出
    }

    /**
     * 启动实例
     */
    @Transactional
    public void start(Long id) {
        HoneypotInstance instance = getAndCheck(id);
        if (InstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            throw new BusinessException(ResultCode.HP_INSTANCE_STATUS_ERROR.getCode(), "实例已在运行中");
        }
        // 发送启动消息
        rabbitTemplate.convertAndSend(MqConstants.DEPLOY_EXCHANGE, MqConstants.START_ROUTING_KEY, id);
        log.info("已发送启动实例 {} 的消息到队列", id);
    }

    /**
     * 停止实例
     */
    @Transactional
    public void stop(Long id) {
        HoneypotInstance instance = getAndCheck(id);
        if (!InstanceStatus.RUNNING.getCode().equals(instance.getStatus())) {
            throw new BusinessException(ResultCode.HP_INSTANCE_STATUS_ERROR.getCode(), "实例未在运行中，无法停止");
        }

        // 发送停止消息（只传 ID）
        rabbitTemplate.convertAndSend(MqConstants.DEPLOY_EXCHANGE, MqConstants.STOP_ROUTING_KEY, id);
        log.info("已发送停止实例 {} 的消息到队列", id);
    }

    /**
     * 销毁实例
     */
    @Transactional
    public void destroy(Long id) {
        HoneypotInstance instance = getAndCheck(id);
        // 检查状态：只有 STOPPED、ERROR、DESTROYED 状态才允许销毁？或者只要是已停止或错误状态即可
        // 若为 RUNNING，需要先停止，但我们可以让 Worker 处理停止逻辑
        // 这里只做校验：不能销毁已经 DESTROYED 的
        if (InstanceStatus.DESTROYED.getCode().equals(instance.getStatus())) {
            throw new BusinessException(ResultCode.HP_INSTANCE_STATUS_ERROR.getCode(), "实例已销毁");
        }
        // 发送销毁消息
        rabbitTemplate.convertAndSend(MqConstants.DEPLOY_EXCHANGE, MqConstants.DESTROY_ROUTING_KEY, id);
        log.info("已发送销毁实例 {} 的消息到队列", id);
    }

    private HoneypotInstance getAndCheck(Long id) {
        HoneypotInstance instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw new BusinessException(ResultCode.HP_INSTANCE_NOT_FOUND);
        }
        if(!instance.getCreateUserId().equals(UserContext.getUserId()))
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        return instance;
    }

    @Transactional
    public void updateStatus(Long id, String code) {
        HoneypotInstance instance = getAndCheck(id);
        instance.setStatus(code);
        instanceMapper.updateById(instance);
    }

    @Transactional
    public void updateDeployOrStopSuccess(Long id, String ip, Integer sshPort, String code) {
        LambdaUpdateWrapper<HoneypotInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HoneypotInstance::getId, id)
                .set(HoneypotInstance::getIpAddress, ip)
                .set(HoneypotInstance::getPort, sshPort)
                .set(HoneypotInstance::getLastHeartbeat, LocalDateTime.now())
                .set(HoneypotInstance::getStatus, code);
        instanceMapper.update(null, wrapper);
    }
}
