package com.whpu.mybs.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whpu.mybs.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
