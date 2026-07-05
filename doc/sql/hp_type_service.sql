-- ============================================
-- 蜜罐类型管理数据库初始化脚本
-- 数据库: db_hp_type_service
-- ============================================

CREATE DATABASE IF NOT EXISTS `db_hp_type_service`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `db_hp_type_service`;

-- 蜜罐类型表
CREATE TABLE IF NOT EXISTS `tb_honeypot_type` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type_uuid` BINARY(16) NOT NULL COMMENT '类型UUID (全局唯一)',
    `type_name` VARCHAR(50) NOT NULL COMMENT '类型名称',
    `image_name` VARCHAR(100) NOT NULL COMMENT '镜像名称',
    `image_id` VARCHAR(100) DEFAULT NULL COMMENT '镜像ID',
    `config` TEXT DEFAULT NULL COMMENT '扩展配置 (JSON格式)',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `min_cpu` INT DEFAULT 1 COMMENT '最小CPU核数',
    `min_memory` INT DEFAULT 1024 COMMENT '最小内存大小 (MB)',
    `min_disk` INT DEFAULT 10 COMMENT '最小磁盘大小 (GB)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_uuid` (`type_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜜罐类型表';
