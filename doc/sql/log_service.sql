-- ============================================
-- 日志管理数据库初始化脚本
-- 数据库: db_log_service
-- ============================================

CREATE DATABASE IF NOT EXISTS `db_log_service`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `db_log_service`;

-- 攻击日志表
CREATE TABLE IF NOT EXISTS `tb_attack_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `instance_id` BIGINT NOT NULL COMMENT '蜜罐实例ID',
    `instance_name` VARCHAR(100) DEFAULT NULL COMMENT '蜜罐实例名称 (冗余)',
    `attacker_ip` VARCHAR(45) NOT NULL COMMENT '攻击来源IP',
    `source_port` INT DEFAULT NULL COMMENT '攻击来源端口',
    `attack_type` VARCHAR(50) DEFAULT NULL COMMENT '攻击类型 (BRUTE_FORCE, CVE_EXPLOIT, SCAN...)',
    `attack_payload` TEXT DEFAULT NULL COMMENT '攻击载荷',
    `raw_data` TEXT DEFAULT NULL COMMENT '原始数据',
    `geo_location` VARCHAR(200) DEFAULT NULL COMMENT '地理位置',
    `attack_time` DATETIME NOT NULL COMMENT '攻击时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_attacker_ip` (`attacker_ip`),
    KEY `idx_attack_type` (`attack_type`),
    KEY `idx_attack_time` (`attack_time`),
    KEY `idx_instance_time` (`instance_id`, `attack_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='攻击日志表';
