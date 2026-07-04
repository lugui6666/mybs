-- ============================================
-- 蜜罐实例管理数据库初始化脚本
-- 数据库: db_hp_instance_service
-- ============================================

CREATE DATABASE IF NOT EXISTS `db_hp_instance_service`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `db_hp_instance_service`;

-- 蜜罐实例表
CREATE TABLE IF NOT EXISTS `tb_honeypot_instance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `instance_name` VARCHAR(100) NOT NULL COMMENT '实例名称',
    `type_id` BIGINT NOT NULL COMMENT '关联的蜜罐类型ID',
    `ip_address` VARCHAR(45) NOT NULL COMMENT '部署IP地址',
    `port` INT NOT NULL COMMENT '运行端口',
    `status` VARCHAR(20) DEFAULT 'deploying' COMMENT '状态: deploying/running/stopped/error/destroyed',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `config_json` TEXT DEFAULT NULL COMMENT '扩展配置 (JSON格式)',
    `container_id` VARCHAR(100) DEFAULT NULL COMMENT '容器ID',
    `deployed_at` DATETIME DEFAULT NULL COMMENT '部署时间',
    `last_heartbeat` DATETIME DEFAULT NULL COMMENT '最后心跳时间',
    `create_user_id` BIGINT DEFAULT NULL COMMENT '创建者用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_type_id` (`type_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_user` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜜罐实例表';
