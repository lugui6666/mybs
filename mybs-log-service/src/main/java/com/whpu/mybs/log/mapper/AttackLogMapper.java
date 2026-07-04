package com.whpu.mybs.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whpu.mybs.log.entity.AttackLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 攻击日志 Mapper
 */
@Mapper
public interface AttackLogMapper extends BaseMapper<AttackLog> {
}
