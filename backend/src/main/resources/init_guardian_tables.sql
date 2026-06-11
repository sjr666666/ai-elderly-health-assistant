-- =====================================================
-- 家属端相关表初始化脚本
-- =====================================================
-- 包含: guardian_elder_relation / sms_notification_log / emergency_event
-- 说明: 必须在 init_database.sql 之后执行（依赖 sys_user 表）
-- =====================================================

USE `elderly_medication`;

-- 设置客户端字符集
SET NAMES utf8mb4;

-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- ==================== 家属-老人关联表 ====================
CREATE TABLE IF NOT EXISTS `guardian_elder_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `guardian_id` bigint NOT NULL COMMENT '家属ID，关联sys_user表',
  `elder_id` bigint NOT NULL COMMENT '老人ID，关联sys_user表',
  `relation_type` varchar(50) NULL COMMENT '关系类型，如子女/配偶/护工',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/inactive',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_guardian_id` (`guardian_id`),
  INDEX `idx_elder_id` (`elder_id`),
  UNIQUE KEY `uk_guardian_elder` (`guardian_id`, `elder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家属-老人关联表';

-- ==================== 短信通知日志表 ====================
CREATE TABLE IF NOT EXISTS `sms_notification_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `guardian_id` bigint NOT NULL COMMENT '家属ID，关联sys_user表',
  `elder_id` bigint NOT NULL COMMENT '老人ID，关联sys_user表',
  `phone` varchar(20) NOT NULL COMMENT '接收手机号',
  `sms_type` varchar(50) NOT NULL COMMENT '短信类型，如missed_dose_alert/emergency_alert/expiring_drug_reminder',
  `content` text NOT NULL COMMENT '短信内容',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '发送状态：pending/sent/failed',
  `provider_msg_id` varchar(100) NULL COMMENT '短信服务商消息ID',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `sent_at` datetime NULL COMMENT '发送时间',
  `error_message` varchar(500) NULL COMMENT '错误信息',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_sms_guardian` (`guardian_id`),
  INDEX `idx_sms_elder` (`elder_id`),
  INDEX `idx_sms_status` (`status`),
  INDEX `idx_sms_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信通知日志表';

-- ==================== 紧急事件表 ====================
CREATE TABLE IF NOT EXISTS `emergency_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `elder_id` bigint NOT NULL COMMENT '老人ID，关联sys_user表',
  `event_type` varchar(50) NOT NULL COMMENT '事件类型：fall/sos/abnormal/medication_missed/other',
  `severity` varchar(20) NOT NULL DEFAULT 'medium' COMMENT '严重程度：low/medium/high',
  `description` text NULL COMMENT '事件描述',
  `event_time` datetime NOT NULL COMMENT '事件发生时间',
  `is_resolved` tinyint NOT NULL DEFAULT 0 COMMENT '是否已处理：0-未处理 1-已处理',
  `resolved_by` bigint NULL COMMENT '处理人ID',
  `resolved_at` datetime NULL COMMENT '处理时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_event_elder` (`elder_id`),
  INDEX `idx_event_resolved` (`is_resolved`),
  INDEX `idx_event_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='紧急事件表';

-- =============== 添加外键约束 ===============
-- 使用存储过程安全添加外键（避免重复执行报错）
DROP PROCEDURE IF EXISTS add_fk_if_not_exists;
DELIMITER $$
CREATE PROCEDURE add_fk_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_constraint VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND CONSTRAINT_NAME = p_constraint
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD CONSTRAINT `', p_constraint, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_fk_if_not_exists('guardian_elder_relation', 'fk_relation_guardian', 'FOREIGN KEY (`guardian_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('guardian_elder_relation', 'fk_relation_elder', 'FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('sms_notification_log', 'fk_sms_guardian', 'FOREIGN KEY (`guardian_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('sms_notification_log', 'fk_sms_elder', 'FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('emergency_event', 'fk_event_elder', 'FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');

DROP PROCEDURE IF EXISTS add_fk_if_not_exists;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 输出完成信息
SELECT '家属端数据表初始化完成！' AS result;
