package com.whpu.mybs.common.enums;

import lombok.Getter;

/**
 * 蜜罐实例状态枚举
 */
@Getter
public enum InstanceStatus {

    DEPLOYING("deploying", "部署中"),
    RUNNING("running", "运行中"),
    STOPPED("stopped", "已停止"),
    ERROR("error", "异常"),
    DESTROYED("destroyed", "已销毁");

    private final String code;
    private final String description;

    InstanceStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static InstanceStatus fromCode(String code) {
        for (InstanceStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }

}
