package com.whpu.mybs.hptype.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新类型请求
 */
@Data
public class UpdateTypeRequest {
    @NotBlank(message = "镜像名称不能为空")
    private String imageName;

    @NotBlank(message = "配置不能为空")
    private String config;

    private String description;

    private Integer minCpu;

    private Integer minMemory;

    private Integer minDisk;
}
