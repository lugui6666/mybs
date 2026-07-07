package com.whpu.mybs.common.dto;

import lombok.Data;

import java.io.PrintStream;

@Data
public class HoneypotTypeDTO {
    private Long id;
    private String imageName;
    private Integer minCpu;
    private Integer minMemory;
    // 按需添加其它字段
}