package com.whpu.mybs.hpinstance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whpu.mybs.common.enums.InstanceStatus;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.hpinstance.entity.HoneypotInstance;
import com.whpu.mybs.hpinstance.mapper.HoneypotInstanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 蜜罐实例服务
 */
@Service
@RequiredArgsConstructor
public class HoneypotInstanceService extends ServiceImpl<HoneypotInstanceMapper, HoneypotInstance> {

    private final HoneypotInstanceMapper instanceMapper;

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
    public void deploy(HoneypotInstance instance) {
        instance.setStatus(InstanceStatus.DEPLOYING.getCode());
        instance.setDeployedAt(LocalDateTime.now());
        instanceMapper.insert(instance);

        // 模拟部署成功 → 变为运行中
        instance.setStatus(InstanceStatus.RUNNING.getCode());
        instance.setLastHeartbeat(LocalDateTime.now());
        instanceMapper.updateById(instance);
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
        instance.setStatus(InstanceStatus.RUNNING.getCode());
        instance.setLastHeartbeat(LocalDateTime.now());
        instanceMapper.updateById(instance);
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
        instance.setStatus(InstanceStatus.STOPPED.getCode());
        instanceMapper.updateById(instance);
    }

    /**
     * 销毁实例
     */
    @Transactional
    public void destroy(Long id) {
        HoneypotInstance instance = getAndCheck(id);
        instance.setStatus(InstanceStatus.DESTROYED.getCode());
        instanceMapper.updateById(instance);
    }

    private HoneypotInstance getAndCheck(Long id) {
        HoneypotInstance instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw new BusinessException(ResultCode.HP_INSTANCE_NOT_FOUND);
        }
        return instance;
    }

}
