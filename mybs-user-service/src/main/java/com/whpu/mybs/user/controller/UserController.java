package com.whpu.mybs.user.controller;

import com.whpu.mybs.common.dto.R;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.common.utils.UserContext;
import com.whpu.mybs.user.entity.User;
import com.whpu.mybs.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 判断当前token是否与被修改信息的用户匹配
    private void checkUserId(Long id) {
        Long userId = UserContext.getUserId();
        if (!userId.equals(id)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }


    /**
     * 查询用户详情
     */
    @GetMapping("/{id}")
    public R<User> getById(@PathVariable Long id) {
        checkUserId(id);
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(null);
        return R.ok(user);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody User user) {
        checkUserId(id);
        User existing = userService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setId(id);
        userService.updateById(user);
        return R.ok("更新用户成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        checkUserId(id);
        User existing = userService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        userService.removeById(id);
        UserContext.clear();
        return R.ok("删除用户成功");
    }

}
