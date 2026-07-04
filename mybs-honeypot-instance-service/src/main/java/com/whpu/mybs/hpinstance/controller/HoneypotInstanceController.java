package com.whpu.mybs.hpinstance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whpu.mybs.common.dto.PageResult;
import com.whpu.mybs.common.dto.R;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.hpinstance.entity.HoneypotInstance;
import com.whpu.mybs.hpinstance.service.HoneypotInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 蜜罐实例管理控制器
 */
@RestController
@RequestMapping("/api/hp-instance")
@RequiredArgsConstructor
public class HoneypotInstanceController {

    private final HoneypotInstanceService instanceService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<PageResult<HoneypotInstance>> page(@RequestParam(defaultValue = "1") Long page,
                                                  @RequestParam(defaultValue = "10") Long size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) Long typeId) {
        Page<HoneypotInstance> pageResult =
                instanceService.page(new Page<>(page, size), keyword, status, typeId);
        return R.ok(PageResult.of(pageResult.getRecords(), pageResult.getTotal(), page, size));
    }

    /**
     * 查询详情
     */
    @GetMapping("/{id}")
    public R<HoneypotInstance> getById(@PathVariable Long id) {
        HoneypotInstance instance = instanceService.getById(id);
        if (instance == null) {
            throw new BusinessException(ResultCode.HP_INSTANCE_NOT_FOUND);
        }
        return R.ok(instance);
    }

    /**
     * 创建/部署实例
     */
    @PostMapping
    public R<Void> deploy(@RequestBody HoneypotInstance instance) {
        instance.setId(null);
        instanceService.deploy(instance);
        return R.ok("部署成功");
    }

    /**
     * 更新实例配置
     */
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody HoneypotInstance instance) {
        HoneypotInstance existing = instanceService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.HP_INSTANCE_NOT_FOUND);
        }
        instance.setId(id);
        instanceService.updateById(instance);
        return R.ok("更新实例成功");
    }

    /**
     * 启动实例
     */
    @PutMapping("/{id}/start")
    public R<Void> start(@PathVariable Long id) {
        instanceService.start(id);
        return R.ok("启动成功");
    }

    /**
     * 停止实例
     */
    @PutMapping("/{id}/stop")
    public R<Void> stop(@PathVariable Long id) {
        instanceService.stop(id);
        return R.ok("停止成功");
    }

    /**
     * 销毁实例
     */
    @DeleteMapping("/{id}")
    public R<Void> destroy(@PathVariable Long id) {
        instanceService.destroy(id);
        return R.ok("销毁成功");
    }

}
