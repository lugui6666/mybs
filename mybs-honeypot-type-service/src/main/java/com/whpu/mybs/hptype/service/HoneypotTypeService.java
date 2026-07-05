package com.whpu.mybs.hptype.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.common.utils.JsonValidator;
import com.whpu.mybs.hptype.dto.CreateTypeRequest;
import com.whpu.mybs.hptype.dto.UpdateTypeRequest;
import com.whpu.mybs.hptype.entity.HoneypotType;
import com.whpu.mybs.hptype.mapper.HoneypotTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * 蜜罐类型服务
 */
@Service
@RequiredArgsConstructor
public class HoneypotTypeService extends ServiceImpl<HoneypotTypeMapper, HoneypotType> {

    private final HoneypotTypeMapper typeMapper;

    /**
     * 分页查询
     */
    public Page<HoneypotType> page(Page<HoneypotType> page, String keyword) {
        LambdaQueryWrapper<HoneypotType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(HoneypotType::getTypeName, keyword)
                    .or()
                    .like(HoneypotType::getTypeUuid, keyword);
        }
        wrapper.orderByDesc(HoneypotType::getCreateTime);
        return typeMapper.selectPage(page, wrapper);
    }

    /**
     * 全部列表
     */
    public List<HoneypotType> listAll() {
        LambdaQueryWrapper<HoneypotType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(HoneypotType::getCreateTime);
        return typeMapper.selectList(wrapper);
    }

    /**
     * 新增
     */
    public void createType(CreateTypeRequest request) {
        HoneypotType type = new HoneypotType();
        // 生成唯一的uuid设置给type
        type.setTypeUuid(UUID.randomUUID().toString());

        // 生成img-开头后面跟随8位随机字符的imageid
        type.setImageId("img-" + UUID.randomUUID().toString().substring(0, 8));

        // 使用Jackson库检查config的json格式是否合法
        if (JsonValidator.isValidJson(type.getConfig())) {
            throw new BusinessException(ResultCode.HP_TYPE_CONFIG_ERROR);
        }
        type.setTypeName(request.getTypeName());
        type.setImageName(request.getImageName());
        type.setConfig(request.getConfig());
        type.setDescription(request.getDescription());
        type.setMinCpu(request.getMinCpu());
        type.setMinMemory(request.getMinMemory());
        type.setMinDisk(request.getMinDisk());
        typeMapper.insert(type);
    }

    public void updateType(Long id, UpdateTypeRequest request) {
        HoneypotType type = new HoneypotType();
        type.setId(id);
        type.setTypeName(request.getImageName());
        type.setImageId("img-" + UUID.randomUUID().toString().substring(0, 8));
        type.setConfig(request.getConfig());
        type.setDescription(request.getDescription());
        type.setMinCpu(request.getMinCpu());
        type.setMinMemory(request.getMinMemory());
        type.setMinDisk(request.getMinDisk());
        typeMapper.updateById(type);
    }
}
