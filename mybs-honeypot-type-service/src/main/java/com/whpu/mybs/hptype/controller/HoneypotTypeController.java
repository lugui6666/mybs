package com.whpu.mybs.hptype.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whpu.mybs.common.dto.PageResult;
import com.whpu.mybs.common.dto.R;
import com.whpu.mybs.common.enums.ResultCode;
import com.whpu.mybs.common.exception.BusinessException;
import com.whpu.mybs.common.utils.JsonValidator;
import com.whpu.mybs.hptype.dto.CreateTypeRequest;
import com.whpu.mybs.hptype.entity.HoneypotType;
import com.whpu.mybs.hptype.service.HoneypotTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 蜜罐类型管理控制器
 */
@RestController
@RequestMapping("/api/hp-type")
@RequiredArgsConstructor
public class HoneypotTypeController {

    private final HoneypotTypeService typeService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<PageResult<HoneypotType>> page(@RequestParam(defaultValue = "1") Long page,
                                             @RequestParam(defaultValue = "10") Long size,
                                             @RequestParam(required = false) String keyword) {
        Page<HoneypotType> pageResult = typeService.page(new Page<>(page, size), keyword);
        return R.ok(PageResult.of(pageResult.getRecords(), pageResult.getTotal(), page, size));
    }

    /**
     * 全部类型列表
     */
    @GetMapping("/list")
    public R<List<HoneypotType>> list() {
        return R.ok(typeService.listAll());
    }

    /**
     * 查询详情
     */
    @GetMapping("/{id}")
    public R<HoneypotType> getById(@PathVariable Long id) {
        HoneypotType type = typeService.getById(id);
        if (type == null) {
            throw new BusinessException(ResultCode.HP_TYPE_NOT_FOUND);
        }
        return R.ok(type);
    }

    /**
     * 新增类型
     */
    @PostMapping
    public R<Void> create(@RequestBody CreateTypeRequest request) {
        typeService.createType(request);
        return R.ok("新增蜜罐类型成功");
    }

    /**
     * 更新类型
     */
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody HoneypotType type) {
        HoneypotType existing = typeService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.HP_TYPE_NOT_FOUND);
        }
        if (!JsonValidator.isValidJson(type.getConfig())) {
            throw new BusinessException(ResultCode.HP_TYPE_CONFIG_ERROR);
        }
        type.setId(id);
        typeService.updateById(type);
        return R.ok("更新蜜罐类型成功");
    }

    /**
     * 删除类型
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        HoneypotType existing = typeService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.HP_TYPE_NOT_FOUND);
        }
        typeService.removeById(id);
        return R.ok("删除蜜罐类型成功");
    }

}
