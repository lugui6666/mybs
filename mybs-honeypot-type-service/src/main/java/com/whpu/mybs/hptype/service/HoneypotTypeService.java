package com.whpu.mybs.hptype.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.hptype.entity.HoneypotType;
import com.whpu.mybs.hptype.mapper.HoneypotTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

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
                    .like(HoneypotType::getTypeCode, keyword);
        }
        wrapper.orderByDesc(HoneypotType::getCreateTime);
        return typeMapper.selectPage(page, wrapper);
    }

    /**
     * 全部列表
     */
    public List<HoneypotType> listAll() {
        LambdaQueryWrapper<HoneypotType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HoneypotType::getEnabled, 1)
                .orderByDesc(HoneypotType::getCreateTime);
        return typeMapper.selectList(wrapper);
    }

    /**
     * 新增
     */
    public void createType(HoneypotType type) {
        // 检查编码唯一性
        LambdaQueryWrapper<HoneypotType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HoneypotType::getTypeCode, type.getTypeCode());
        if (typeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.HP_TYPE_CODE_EXISTS);
        }
        typeMapper.insert(type);
    }

}
