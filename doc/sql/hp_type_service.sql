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
    `type_name` VARCHAR(100) NOT NULL COMMENT '类型名称',
    `type_code` VARCHAR(50) NOT NULL COMMENT '类型编码 (SSH, HTTP, MYSQL...)',
    `protocol` VARCHAR(20) NOT NULL COMMENT '协议 (TCP, UDP, HTTP)',
    `default_port` INT NOT NULL COMMENT '默认端口',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `icon` VARCHAR(200) DEFAULT NULL COMMENT '图标',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 1=启用, 0=禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='蜜罐类型表';

-- ============================================
-- 初始化数据 — 常见蜜罐类型
-- ============================================
INSERT INTO `tb_honeypot_type` (`type_name`, `type_code`, `protocol`, `default_port`, `description`) VALUES
('SSH 蜜罐', 'SSH', 'TCP', 22, '模拟 SSH 服务，捕获暴力破解和漏洞利用攻击'),
('HTTP 蜜罐', 'HTTP', 'TCP', 80, '模拟 Web 服务，捕获 Web 攻击和扫描行为'),
('HTTPS 蜜罐', 'HTTPS', 'TCP', 443, '模拟 HTTPS 服务，捕获加密流量攻击'),
('MySQL 蜜罐', 'MYSQL', 'TCP', 3306, '模拟 MySQL 数据库服务，捕获数据库攻击'),
('Redis 蜜罐', 'REDIS', 'TCP', 6379, '模拟 Redis 缓存服务，捕获未授权访问攻击'),
('FTP 蜜罐', 'FTP', 'TCP', 21, '模拟 FTP 文件服务，捕获文件传输攻击'),
('Telnet 蜜罐', 'TELNET', 'TCP', 23, '模拟 Telnet 服务，捕获 IoT 设备攻击'),
('SMB 蜜罐', 'SMB', 'TCP', 445, '模拟 Windows 文件共享，捕获勒索病毒传播'),
('RDP 蜜罐', 'RDP', 'TCP', 3389, '模拟 Windows 远程桌面，捕获远程登录攻击'),
('DNS 蜜罐', 'DNS', 'UDP', 53, '模拟 DNS 服务，捕获 DNS 隧道和投毒攻击');
