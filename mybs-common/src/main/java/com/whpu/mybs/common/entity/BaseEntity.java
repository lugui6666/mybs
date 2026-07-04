package com.whpu.mybs.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类 — 所有数据库实体继承此类
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 ID (自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建时间 (自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 (自动填充) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志 (0=未删除, 1=已删除) */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

}
