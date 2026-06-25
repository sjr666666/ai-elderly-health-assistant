-- =====================================================
-- 老年人用药管理系统 - 数据库初始化脚本
-- =====================================================
-- 版本: 2.2
-- 更新内容:
--   1. sys_user 建表语句补充 phone 字段（与 add_col_if_missing 保持一致）
--   2. drug_category_keywords/drug_aliases 改用 CREATE TABLE IF NOT EXISTS
--      配合 INSERT IGNORE 实现脚本幂等（可重复执行不报错）
--   3. 保留原有的 add_col_if_missing 存储过程兼容老库
--   4. 其余业务表仍使用 DROP+CREATE，确保数据干净
-- =====================================================
-- 执行方式:
--   MySQL 命令行: source /path/to/init_database.sql
--   Navicat:      打开脚本 → 运行（已自动处理 DELIMITER）
--   DBeaver:      右键脚本 → Execute → 勾选 "Ignore DELIMITER"
--   注意: 必须按顺序执行: 1.init_database.sql → 2.init_drug_data.sql → 3.init_guardian_tables.sql
-- =====================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `elderly_medication`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `elderly_medication`;

-- 设置客户端字符集
SET NAMES utf8mb4;

-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- ==================== 用户表 ====================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `user_id` bigint NOT NULL UNIQUE COMMENT '用户ID（雪花算法生成）',
  `username` varchar(50) NOT NULL UNIQUE COMMENT '登录名',
  `password` varchar(255) NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名/称呼',
  `phone` varchar(20) NULL COMMENT '手机号',
  `age` tinyint NULL COMMENT '年龄',
  `gender` varchar(10) NULL COMMENT '性别：male/female',
  `height` decimal(5,1) NULL COMMENT '身高（cm）',
  `weight` decimal(5,1) NULL COMMENT '体重（kg）',
  `allergy_history` text NULL COMMENT '过敏史描述',
  `chronic_diseases` text NULL COMMENT '慢性病史描述',
  `kidney_function` varchar(50) NULL COMMENT '肾功能状态：normal/mild_impairment/moderate_impairment/severe_impairment/unknown',
  `liver_function` varchar(50) NULL COMMENT '肝功能状态：normal/mild_impairment/moderate_impairment/severe_impairment/unknown',
  `is_pregnant` tinyint NOT NULL DEFAULT 0 COMMENT '是否孕期：0否/1是',
  `is_breastfeeding` tinyint NOT NULL DEFAULT 0 COMMENT '是否哺乳期：0否/1是',
  `is_smoking` tinyint NOT NULL DEFAULT 0 COMMENT '是否吸烟：0否/1是',
  `is_drinking` tinyint NOT NULL DEFAULT 0 COMMENT '是否饮酒：0否/1是',
  `role` varchar(20) NOT NULL DEFAULT 'elder' COMMENT '角色：elder/family',
  `last_active_time` datetime NULL COMMENT '最后活跃时间',
  `bind_elder_id` bigint NULL COMMENT '家属绑定的老人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ==================== 用户表-扩展字段平滑升级（已存在库兼容） ====================
-- 兼容老库：通过存储过程判断列是否存在再 ADD（MySQL 8.0.x 不支持 ADD COLUMN IF NOT EXISTS）
DROP PROCEDURE IF EXISTS add_col_if_missing;
DELIMITER $$
CREATE PROCEDURE add_col_if_missing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_col_if_missing('sys_user', 'gender',         "varchar(10) NULL COMMENT '性别：male/female' AFTER `age`");
CALL add_col_if_missing('sys_user', 'height',         "decimal(5,1) NULL COMMENT '身高（cm）' AFTER `gender`");
CALL add_col_if_missing('sys_user', 'weight',         "decimal(5,1) NULL COMMENT '体重（kg）' AFTER `height`");
CALL add_col_if_missing('sys_user', 'kidney_function',"varchar(50) NULL COMMENT '肾功能状态' AFTER `chronic_diseases`");
CALL add_col_if_missing('sys_user', 'liver_function', "varchar(50) NULL COMMENT '肝功能状态' AFTER `kidney_function`");
CALL add_col_if_missing('sys_user', 'is_pregnant',    "tinyint NOT NULL DEFAULT 0 COMMENT '是否孕期' AFTER `liver_function`");
CALL add_col_if_missing('sys_user', 'is_breastfeeding',"tinyint NOT NULL DEFAULT 0 COMMENT '是否哺乳期' AFTER `is_pregnant`");
CALL add_col_if_missing('sys_user', 'is_smoking',     "tinyint NOT NULL DEFAULT 0 COMMENT '是否吸烟' AFTER `is_breastfeeding`");
CALL add_col_if_missing('sys_user', 'phone',          "varchar(20) NULL COMMENT '手机号' AFTER `real_name`");
CALL add_col_if_missing('sys_user', 'is_drinking',    "tinyint NOT NULL DEFAULT 0 COMMENT '是否饮酒' AFTER `is_smoking`");
CALL add_col_if_missing('sys_user', 'last_active_time',"datetime NULL COMMENT '最后活跃时间' AFTER `role`");

DROP PROCEDURE add_col_if_missing;

-- ==================== 药品基础库表 ====================
DROP TABLE IF EXISTS `drug_base`;
CREATE TABLE `drug_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  `approval_number` varchar(100) UNIQUE COMMENT '国药准字',
  `generic_name` varchar(200) NOT NULL COMMENT '通用名',
  `trade_name` varchar(200) NULL COMMENT '商品名',
  `common_name` varchar(200) NULL COMMENT '俗名/别名',
  `specification` varchar(100) NULL COMMENT '规格',
  `manufacturer` varchar(200) NULL COMMENT '生产厂家',
  `category` varchar(100) NULL COMMENT '药品分类',
  `description` text NULL COMMENT '药品说明原文',
  `image_url` varchar(500) NULL COMMENT '药品标准图片',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_approval_number` (`approval_number`),
  INDEX `idx_generic_name` (`generic_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品基础库表';

-- ==================== 家庭药箱表 ====================
DROP TABLE IF EXISTS `user_medicine_box`;
CREATE TABLE `user_medicine_box` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '所属老人',
  `drug_id` bigint NULL COMMENT '药品',
  `dosage` varchar(50) NOT NULL COMMENT '每次用量',
  `frequency` varchar(50) NOT NULL COMMENT '频率',
  `start_date` date NULL COMMENT '开始服用日期',
  `end_date` date NULL COMMENT '预计结束日期',
  `expiry_date` date NULL COMMENT '药品有效期',
  `total_quantity` int NULL COMMENT '总数量',
  `remaining_quantity` int NULL COMMENT '剩余数量',
  `note` varchar(500) NULL COMMENT '用户备注',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/stopped',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_status` (`user_id`, `status`),
  INDEX `idx_drug_id` (`drug_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭药箱表';

-- ==================== OCR识别记录表 ====================
DROP TABLE IF EXISTS `ocr_record`;
CREATE TABLE `ocr_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '上传用户',
  `image_url` varchar(500) NOT NULL COMMENT '图片存储路径',
  `raw_text` text NULL COMMENT 'OCR原始识别文本',
  `matched_drug_id` bigint NULL COMMENT '匹配到的药品ID',
  `match_score` decimal(5,4) NULL COMMENT '匹配置信度',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/matched/unmatched/failed',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  INDEX `idx_ocr_user_time` (`user_id`, `created_at`),
  INDEX `idx_ocr_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OCR识别记录表';

-- ==================== 药品识别日志表 ====================
DROP TABLE IF EXISTS `drug_recognition_log`;
CREATE TABLE `drug_recognition_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `ocr_record_id` bigint NULL COMMENT '关联OCR记录',
  `user_id` bigint NULL COMMENT '用户ID',
  `raw_text` text NULL COMMENT '原始识别文本',
  `normalized_name` varchar(255) NULL COMMENT '标准化后的名称',
  `matched_drug_id` bigint NULL COMMENT '匹配的药品ID',
  `matched_drug_name` varchar(255) NULL COMMENT '匹配到的药品名称',
  `match_score` decimal(5,4) NULL COMMENT '匹配分数',
  `matched` tinyint(1) DEFAULT 0 COMMENT '是否匹配成功',
  `auto_imported` tinyint(1) DEFAULT 0 COMMENT '是否自动入库',
  `imported_drug_id` bigint NULL COMMENT '新入库的药品ID',
  `status` varchar(50) NULL DEFAULT '' COMMENT '识别状态',
  `remark` varchar(500) NULL COMMENT '备注信息',
  `is_new_drug` tinyint(1) DEFAULT 0 COMMENT '是否新药品入库',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_rec_log_ocr` (`ocr_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品识别日志表';

-- ==================== AI对话记录表 ====================
DROP TABLE IF EXISTS `ai_conversation_log`;
CREATE TABLE `ai_conversation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '对话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `query_type` varchar(50) NOT NULL COMMENT '查询类型',
  `user_input` text NOT NULL COMMENT '用户输入',
  `ai_output` text NULL COMMENT 'AI返回内容',
  `safety_check_passed` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否通过安全检查',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '对话时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_ai_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话记录表';

-- ==================== 用药计划表 ====================
DROP TABLE IF EXISTS `medication_plan`;
CREATE TABLE `medication_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '老人ID',
  `drug_id` bigint NOT NULL COMMENT '药品ID',
  `box_item_id` bigint NULL COMMENT '关联药箱条目ID',
  `plan_date` date NOT NULL COMMENT '计划日期',
  `time_slot` varchar(20) NOT NULL COMMENT '时段',
  `dosage_at_time` varchar(50) NOT NULL COMMENT '该时段用量',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态',
  `remind_before` int NULL DEFAULT 15 COMMENT '提前提醒分钟数',
  `reminder_stage` varchar(20) NOT NULL DEFAULT 'none' COMMENT '提醒阶段: none/pre_remind/due_now/overdue/notify_family',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_plan_user_date` (`user_id`, `plan_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药计划表';

-- ==================== 服药确认记录表 ====================
DROP TABLE IF EXISTS `medication_log`;
CREATE TABLE `medication_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `plan_id` bigint NOT NULL COMMENT '计划明细ID',
  `user_id` bigint NOT NULL COMMENT '老人ID',
  `status` varchar(20) NOT NULL COMMENT '状态',
  `confirmed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '确认时间',
  `note` varchar(500) NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_log_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服药确认记录表';

-- ==================== 提醒通知记录表 ====================
DROP TABLE IF EXISTS `reminder_log`;
CREATE TABLE `reminder_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '目标老人ID',
  `plan_id` bigint NULL COMMENT '关联计划ID',
  `remind_type` varchar(50) NOT NULL COMMENT '提醒类型',
  `content` text NOT NULL COMMENT '提醒内容文本',
  `channel` varchar(50) NOT NULL COMMENT '渠道',
  `status` varchar(20) NOT NULL DEFAULT 'sent' COMMENT '状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_remind_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒通知记录表';

-- ==================== 紧急联系人表 ====================
DROP TABLE IF EXISTS `emergency_contact`;
CREATE TABLE `emergency_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '联系人ID',
  `elder_id` bigint NOT NULL COMMENT '所属老人ID',
  `name` varchar(100) NOT NULL COMMENT '联系人姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `email` varchar(100) NULL COMMENT '邮箱',
  `relationship` varchar(50) NULL COMMENT '关系',
  `is_primary` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否主要联系人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_contact_elder` (`elder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='紧急联系人表';

-- ==================== 药品冲突规则表 ====================
DROP TABLE IF EXISTS `drug_conflict_rules`;
CREATE TABLE `drug_conflict_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `drug_a_id` bigint NOT NULL COMMENT '药品A的ID',
  `drug_b_id` bigint NOT NULL COMMENT '药品B的ID',
  `conflict_level` varchar(20) NOT NULL COMMENT '冲突等级',
  `conflict_reason` text NULL COMMENT '冲突原因',
  `conflict_reason_plain` text NULL COMMENT '白话版冲突原因',
  `source` varchar(100) NULL COMMENT '数据来源',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_drug_pair` (`drug_a_id`, `drug_b_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品冲突规则表';

-- ==================== 药品类别关键词表 ====================
CREATE TABLE IF NOT EXISTS `drug_category_keywords` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `category` varchar(100) NOT NULL COMMENT '药品类别',
  `keyword` varchar(100) NOT NULL COMMENT '搜索关键词',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_keyword` (`category`, `keyword`),
  INDEX `idx_category` (`category`),
  INDEX `idx_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品类别关键词表';

-- ==================== 药品别名映射表 ====================
CREATE TABLE IF NOT EXISTS `drug_aliases` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `alias_name` varchar(200) NOT NULL COMMENT '药品别名',
  `generic_name` varchar(200) NOT NULL COMMENT '对应的通用名',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alias` (`alias_name`),
  INDEX `idx_generic_name` (`generic_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品别名映射表';

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

CALL add_fk_if_not_exists('user_medicine_box', 'fk_box_user', 'FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('user_medicine_box', 'fk_box_drug', 'FOREIGN KEY (`drug_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('ocr_record', 'fk_ocr_user', 'FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('ocr_record', 'fk_ocr_drug', 'FOREIGN KEY (`matched_drug_id`) REFERENCES `drug_base`(`id`) ON DELETE SET NULL');
CALL add_fk_if_not_exists('drug_recognition_log', 'fk_rec_log_ocr', 'FOREIGN KEY (`ocr_record_id`) REFERENCES `ocr_record`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('medication_plan', 'fk_plan_user', 'FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('medication_plan', 'fk_plan_drug', 'FOREIGN KEY (`drug_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('medication_log', 'fk_log_plan', 'FOREIGN KEY (`plan_id`) REFERENCES `medication_plan`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('medication_log', 'fk_log_user', 'FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('reminder_log', 'fk_remind_user', 'FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('emergency_contact', 'fk_contact_elder', 'FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('drug_conflict_rules', 'fk_conflict_drug_a', 'FOREIGN KEY (`drug_a_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE');
CALL add_fk_if_not_exists('drug_conflict_rules', 'fk_conflict_drug_b', 'FOREIGN KEY (`drug_b_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE');

-- ==================== 今日一课-慢病科普表 ====================
CREATE TABLE IF NOT EXISTS `daily_lesson` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（关联sys_user.id）',
  `lesson_date` date NOT NULL COMMENT '推送日期',
  `chronic_disease` varchar(100) NULL COMMENT '本次科普针对的慢病名称',
  `title` varchar(200) NULL COMMENT '科普标题',
  `content` text NULL COMMENT '科普正文（200-350字）',
  `is_generated` tinyint NOT NULL DEFAULT 0 COMMENT '0-未生成或生成失败 1-已生成',
  `error_msg` varchar(500) NULL COMMENT 'AI生成失败时的错误信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `lesson_date`),
  INDEX `idx_lesson_date` (`lesson_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='今日一课-慢病科普表';

CALL add_fk_if_not_exists('daily_lesson', 'fk_daily_lesson_user', 'FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE');

DROP PROCEDURE IF EXISTS add_fk_if_not_exists;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- =============== 插入测试数据 ===============

-- ---------------- 用户测试数据 ----------------
-- 密码统一为: 123456 (BCrypt 哈希，$2a$ 10 轮)
-- 使用 INSERT IGNORE 避免 sys_user 已存在时 UNIQUE(user_id)/UNIQUE(username) 冲突
-- 这样脚本可重复执行不报错：首次插入，重复执行时跳过已存在的用户
INSERT IGNORE INTO `sys_user` (`user_id`, `username`, `password`, `real_name`, `age`, `gender`, `height`, `weight`, `allergy_history`, `chronic_diseases`, `kidney_function`, `liver_function`, `is_pregnant`, `is_breastfeeding`, `is_smoking`, `is_drinking`, `role`) VALUES
(10001, 'laowang', '$2a$10$VCrz02JzMVmFN2p56zcxR.gSlItx/Cn4kgx817j1q1UXrUJXDOHfu', '王阿姨', 68, 'female', 160.0, 62.5, '无药物过敏史', '高血压、糖尿病', 'mild_impairment', 'normal', 0, 0, 0, 0, 'elder'),
(10002, 'zhangsan', '$2a$10$VCrz02JzMVmFN2p56zcxR.gSlItx/Cn4kgx817j1q1UXrUJXDOHfu', '张三', 35, 'male', 175.0, 72.0, NULL, NULL, 'normal', 'normal', 0, 0, 0, 1, 'family'),
(10003, 'laoli', '$2a$10$VCrz02JzMVmFN2p56zcxR.gSlItx/Cn4kgx817j1q1UXrUJXDOHfu', '李大爷', 72, 'male', 168.0, 68.0, '青霉素过敏', '冠心病、脑梗死后遗症', 'moderate_impairment', 'mild_impairment', 0, 0, 1, 0, 'elder'),
(10004, 'zhaosi', '$2a$10$VCrz02JzMVmFN2p56zcxR.gSlItx/Cn4kgx817j1q1UXrUJXDOHfu', '赵四', 42, 'male', 178.0, 80.0, NULL, NULL, 'normal', 'normal', 0, 0, 0, 0, 'family'),
(10005, 'xiaomei', '$2a$10$VCrz02JzMVmFN2p56zcxR.gSlItx/Cn4kgx817j1q1UXrUJXDOHfu', '小美', 28, 'female', 165.0, 55.0, '磺胺过敏', NULL, 'normal', 'normal', 1, 0, 0, 0, 'family');

-- ---------------- 药品基础数据 ----------------
-- 注意：完整药品数据通过 init_drug_data.sql 导入（必须在此脚本之后执行）
-- 此处不再插入药品数据，避免与 init_drug_data.sql 产生重复记录
-- 依赖 drug_id 的测试数据（药箱/OCR/识别日志/用药计划/冲突规则）也移至 init_drug_data.sql

-- ---------------- 紧急联系人测试数据 ----------------
INSERT INTO `emergency_contact` (`elder_id`, `name`, `phone`, `relationship`, `is_primary`) VALUES
(1, '张三', '13800138001', '儿子', 1),
(1, '王小红', '13800138002', '女儿', 0),
(3, '赵六', '13900139001', '儿子', 1);

-- ---------------- 药品类别关键词数据 ----------------
-- 使用 INSERT IGNORE：drug_category_keywords 在该脚本中用 CREATE TABLE IF NOT EXISTS 创建，
-- 重复执行时 UNIQUE(category,keyword) 冲突会被静默跳过，保证幂等性
INSERT IGNORE INTO `drug_category_keywords` (`category`, `keyword`) VALUES
-- 感冒药类
('感冒药', '感冒'), ('感冒药', '流感'), ('感冒药', '发烧'), ('感冒药', '咳嗽'), ('感冒药', '鼻塞'), ('感冒药', '流涕'), ('感冒药', '咽痛'), ('感冒药', '打喷嚏'), ('感冒药', '风寒'), ('感冒药', '风热'), ('感冒药', '感冒灵'),
-- 退烧药类
('退烧药', '退烧'), ('退烧药', '发热'), ('退烧药', '体温高'), ('退烧药', '发烧'), ('退烧药', '退热'),
-- 止痛药类
('止痛药', '疼痛'), ('止痛药', '头痛'), ('止痛药', '牙痛'), ('止痛药', '关节痛'), ('止痛药', '腰痛'), ('止痛药', '止痛'), ('止痛药', '痛经'), ('止痛药', '偏头痛'), ('止痛药', '神经痛'), ('止痛药', '肌肉痛'),
-- 消炎药类
('消炎药', '消炎'), ('消炎药', '抗炎'), ('消炎药', '红肿'), ('消炎药', '发炎'), ('消炎药', '感染'), ('消炎药', '抗生素'), ('消炎药', '抗菌'),
-- 胃肠道用药类
('胃药', '胃'), ('胃药', '胃痛'), ('胃药', '胃酸'), ('胃药', '胃胀'), ('胃药', '消化'), ('胃药', '烧心'), ('胃药', '反酸'), ('胃药', '胃溃疡'), ('胃药', '胃炎'), ('胃药', '腹泻'), ('胃药', '便秘'),
-- 降压药类
('降压药', '血压'), ('降压药', '降压'), ('降压药', '高血压'), ('降压药', '血压高'), ('降压药', '降血压'),
-- 降糖药类
('降糖药', '血糖'), ('降糖药', '降糖'), ('降糖药', '糖尿病'), ('降糖药', '胰岛素'), ('降糖药', '血糖高'),
-- 心脑血管药类
('心脑血管药', '心脏'), ('心脑血管药', '心律'), ('心脑血管药', '心肌'), ('心脑血管药', '脑梗'), ('心脑血管药', '血栓'), ('心脑血管药', '血脂'), ('心脑血管药', '冠心病'), ('心脑血管药', '心绞痛'), ('心脑血管药', '脑供血'),
-- 抗过敏药类
('抗过敏药', '过敏'), ('抗过敏药', '皮肤痒'), ('抗过敏药', '荨麻疹'), ('抗过敏药', '鼻炎'), ('抗过敏药', '打喷嚏'), ('抗过敏药', '皮疹'), ('抗过敏药', '瘙痒'),
-- 止咳化痰药类
('止咳化痰药', '咳嗽'), ('止咳化痰药', '化痰'), ('止咳化痰药', '止咳'), ('止咳化痰药', '痰多'), ('止咳化痰药', '喉咙痒'), ('止咳化痰药', '支气管炎'),
-- 维生素矿物质类
('维生素矿物质', '维生素'), ('维生素矿物质', '钙片'), ('维生素矿物质', '补钙'), ('维生素矿物质', '补锌'), ('维生素矿物质', '补铁'), ('维生素矿物质', '维生素C'), ('维生素矿物质', '多维'),
-- 安神助眠药类
('安神助眠药', '失眠'), ('安神助眠药', '睡眠'), ('安神助眠药', '安眠'), ('安神助眠药', '安神'), ('安神助眠药', '助眠'), ('安神助眠药', '睡不着'),
-- 跌打损伤药类
('跌打损伤药', '摔伤'), ('跌打损伤药', '跌打'), ('跌打损伤药', '扭伤'), ('跌打损伤药', '撞伤'), ('跌打损伤药', '骨折'), ('跌打损伤药', '外伤'), ('跌打损伤药', '瘀血'), ('跌打损伤药', '肿痛'), ('跌打损伤药', '活血'), ('跌打损伤药', '化瘀'),
-- 皮肤用药类
('皮肤用药', '皮肤'), ('皮肤用药', '皮炎'), ('皮肤用药', '湿疹'), ('皮肤用药', '癣'), ('皮肤用药', '脚气'), ('皮肤用药', '瘙痒'), ('皮肤用药', '痤疮'), ('皮肤用药', '痘痘'), ('皮肤用药', '疱疹'),
-- 眼科用药类
('眼科用药', '眼睛'), ('眼科用药', '滴眼液'), ('眼科用药', '眼疲劳'), ('眼科用药', '干涩'), ('眼科用药', '结膜炎'), ('眼科用药', '角膜炎'), ('眼科用药', '视力模糊'),
-- 清热解毒药类
('清热解毒药', '清热'), ('清热解毒药', '解毒'), ('清热解毒药', '上火'), ('清热解毒药', '咽喉痛'), ('清热解毒药', '口疮'), ('清热解毒药', '板蓝根'), ('清热解毒药', '牛黄'),
-- 晕车药类
('晕车药', '晕车'), ('晕车药', '晕船'), ('晕车药', '晕机'), ('晕车药', '恶心'), ('晕车药', '眩晕');

-- ---------------- 药品别名映射数据 ----------------
-- 使用 INSERT IGNORE：drug_aliases 在该脚本中用 CREATE TABLE IF NOT EXISTS 创建，
-- 重复执行时 UNIQUE(alias_name) 冲突会被静默跳过，保证幂等性
INSERT IGNORE INTO `drug_aliases` (`alias_name`, `generic_name`) VALUES
-- 对乙酰氨基酚
('扑热息痛', '对乙酰氨基酚'), ('泰诺', '对乙酰氨基酚'), ('泰诺林', '对乙酰氨基酚'), ('百服宁', '对乙酰氨基酚'), ('必理通', '对乙酰氨基酚'),
-- 布洛芬
('芬必得', '布洛芬'), ('美林', '布洛芬'), ('安瑞克', '布洛芬'), ('芬必得布洛芬', '布洛芬'),
-- 阿司匹林
('乙酰水杨酸', '阿司匹林'), ('拜阿司匹林', '阿司匹林'), ('拜阿司匹灵', '阿司匹林'),
-- 硝苯地平
('心痛定', '硝苯地平'), ('拜新同', '硝苯地平'), ('伲福达', '硝苯地平'),
-- 二甲双胍
('格华止', '二甲双胍'), ('盐酸二甲双胍', '二甲双胍'),
-- 阿莫西林
('阿莫仙', '阿莫西林'), ('安必仙', '阿莫西林'), ('阿莫灵', '阿莫西林'),
-- 头孢克肟
('世福素', '头孢克肟'), ('达力芬', '头孢克肟'),
-- 蒙脱石散
('思密达', '蒙脱石散'), ('蒙脱石', '蒙脱石散'),
-- 奥美拉唑
('洛赛克', '奥美拉唑'), ('奥克', '奥美拉唑'), ('奥美', '奥美拉唑'),
-- 氯雷他定
('开瑞坦', '氯雷他定'), ('息斯敏', '氯雷他定'), ('雷诺敏', '氯雷他定'),
-- 西替利嗪
('仙特明', '盐酸西替利嗪'), ('西可韦', '盐酸西替利嗪'),
-- 氨氯地平
('络活喜', '苯磺酸氨氯地平'), ('安内真', '苯磺酸氨氯地平'),
-- 氨溴索
('沐舒坦', '盐酸氨溴索'), ('氨溴索', '盐酸氨溴索'),
-- 云南白药
('白药', '云南白药'), ('白药气雾剂', '云南白药气雾剂'),
-- 藿香正气
('藿香正气', '藿香正气水'), ('霍香正气', '藿香正气水'),
-- 板蓝根
('板兰根', '板蓝根'),
-- 葡萄糖酸钙
('钙片', '葡萄糖酸钙片'), ('钙尔奇', '钙尔奇D片'),
-- 阿卡波糖
('拜糖平', '阿卡波糖'),
-- 格列齐特
('达美康', '格列齐特'),
-- 辛伐他汀
('舒降之', '辛伐他汀'),
-- 氯吡格雷
('波立维', '硫酸氢氯吡格雷'),
-- 阿托伐他汀
('立普妥', '阿托伐他汀钙'),
-- 丹参滴丸
('丹参滴丸', '复方丹参滴丸'),
-- 银杏叶
('银杏叶', '银杏叶片'),
-- 甲钴胺
('甲钴胺', '甲钴胺片'), ('弥可保', '甲钴胺片'),
-- 多潘立酮
('吗丁啉', '多潘立酮'),
-- 泮托拉唑
('泮托拉唑', '泮托拉唑钠'), ('潘妥洛克', '泮托拉唑钠'),
-- 左氧氟沙星
('可乐必妥', '左氧氟沙星'), ('左克', '左氧氟沙星'),
-- 阿奇霉素
('希舒美', '阿奇霉素'),
-- 罗红霉素
('罗红霉素', '罗红霉素'), ('仁苏', '罗红霉素'),
-- 甲硝唑
('甲硝唑', '甲硝唑'), ('灭滴灵', '甲硝唑'),
-- 替硝唑
('替硝唑', '替硝唑'),
-- 克霉唑
('克霉唑', '克霉唑'), ('凯妮汀', '克霉唑'),
-- 咪康唑
('咪康唑', '咪康唑'), ('达克宁', '咪康唑'),
-- 特比萘芬
('特比萘芬', '特比萘芬'), ('兰美抒', '特比萘芬'),
-- 炉甘石
('炉甘石', '炉甘石洗剂'),
-- 碘伏
('碘伏', '碘伏'), ('碘酒', '碘伏'),
-- 酒精
('酒精', '乙醇'),
-- 生理盐水
('生理盐水', '氯化钠注射液'),
-- 葡萄糖
('葡萄糖', '葡萄糖注射液'),
-- 氯化钾
('氯化钾', '氯化钾'),
-- 维生素C
('维C', '维生素C'), ('维生素C', '维生素C片'),
-- 维生素B
('维B', '复合维生素B'), ('B族', '复合维生素B'),
-- 褪黑素
('褪黑素', '褪黑素片'), ('脑白金', '褪黑素片'),
-- 安神补脑
('安神补脑', '安神补脑液'),
-- 养血安神
('养血安神', '养血安神片'),
-- 红花油
('红花油', '红花油'), ('正红花油', '红花油'),
-- 正骨水
('正骨水', '正骨水'),
-- 扶他林
('扶他林', '双氯芬酸二乙胺'), ('双氯芬酸', '双氯芬酸钠'),
-- 派瑞松
('派瑞松', '曲安奈德益康唑'),
-- 百多邦
('百多邦', '莫匹罗星'),
-- 皮炎平
('皮炎平', '复方醋酸地塞米松'),
-- 珍视明
('珍视明', '四味珍层冰硼滴眼液'), ('珍珠明目', '珍珠明目滴眼液'),
-- 氯霉素
('氯霉素', '氯霉素滴眼液'),
-- 玻璃酸钠
('玻璃酸钠', '玻璃酸钠滴眼液'), ('海露', '玻璃酸钠滴眼液'),
-- 茶苯海明
('茶苯海明', '茶苯海明片'), ('乘晕宁', '茶苯海明片'),
-- 地芬尼多
('地芬尼多', '盐酸地芬尼多'), ('眩晕停', '盐酸地芬尼多'),
-- 清开灵
('清开灵', '清开灵颗粒'),
-- 小儿氨酚黄那敏
('小儿氨酚黄那敏', '小儿氨酚黄那敏颗粒'), ('小快克', '小儿氨酚黄那敏颗粒'),
-- 小儿肺热咳喘
('小儿肺热咳喘', '小儿肺热咳喘口服液'), ('葵花肺热咳喘', '小儿肺热咳喘口服液');

-- 输出完成信息
SELECT '数据库初始化完成！' AS result;
SELECT COUNT(*) AS table_count FROM information_schema.TABLES WHERE TABLE_SCHEMA='elderly_medication' AND TABLE_TYPE='BASE TABLE';

-- 显示各表数据统计
SELECT 'sys_user' AS table_name, COUNT(*) AS row_count FROM sys_user
UNION ALL SELECT 'drug_base', COUNT(*) FROM drug_base
UNION ALL SELECT 'user_medicine_box', COUNT(*) FROM user_medicine_box
UNION ALL SELECT 'ocr_record', COUNT(*) FROM ocr_record
UNION ALL SELECT 'drug_recognition_log', COUNT(*) FROM drug_recognition_log
UNION ALL SELECT 'ai_conversation_log', COUNT(*) FROM ai_conversation_log
UNION ALL SELECT 'medication_plan', COUNT(*) FROM medication_plan
UNION ALL SELECT 'medication_log', COUNT(*) FROM medication_log
UNION ALL SELECT 'reminder_log', COUNT(*) FROM reminder_log
UNION ALL SELECT 'emergency_contact', COUNT(*) FROM emergency_contact
UNION ALL SELECT 'drug_conflict_rules', COUNT(*) FROM drug_conflict_rules
UNION ALL SELECT 'drug_category_keywords', COUNT(*) FROM drug_category_keywords
UNION ALL SELECT 'drug_aliases', COUNT(*) FROM drug_aliases;
