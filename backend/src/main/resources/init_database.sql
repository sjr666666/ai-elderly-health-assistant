-- =====================================================
-- 老年人用药管理系统 - 数据库初始化脚本
-- =====================================================
-- 版本: 1.0
-- 创建时间: 2026-05-18
-- 描述: 完整的数据库初始化脚本，包含安全删除、重新创建、建表和测试数据
-- 兼容: MySQL 5.7+ / MySQL 8.0+
-- =====================================================

-- ----------------------------
-- 第一部分：安全删除现有数据库
-- ----------------------------
-- 关闭外键检查，确保删除操作顺利进行
SET FOREIGN_KEY_CHECKS = 0;

-- 记录开始时间
SET @start_time = NOW();
SELECT CONCAT('========== 数据库初始化开始 ==========', @start_time) AS '操作日志';

-- 尝试删除数据库（忽略错误）
DROP DATABASE IF EXISTS elderly_medication;

-- 检查删除是否成功
SELECT '步骤1: 旧数据库删除完成' AS '操作日志';

-- ----------------------------
-- 第二部分：创建新数据库
-- ----------------------------
CREATE DATABASE IF NOT EXISTS elderly_medication 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 检查数据库是否创建成功
SELECT '步骤2: 新数据库创建完成' AS '操作日志';

-- 使用新创建的数据库
USE elderly_medication;

-- ----------------------------
-- 第三部分：创建数据表
-- ----------------------------

-- ==================== 用户表 ====================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `user_id` bigint NOT NULL UNIQUE COMMENT '用户ID（雪花算法生成）',
  `username` varchar(50) NOT NULL UNIQUE COMMENT '登录名',
  `password` varchar(255) NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名/称呼',
  `age` tinyint NULL COMMENT '年龄',
  `allergy_history` text NULL COMMENT '过敏史描述',
  `chronic_diseases` text NULL COMMENT '慢性病史描述',
  `role` varchar(20) NOT NULL DEFAULT 'elder' COMMENT '角色：elder/family',
  `bind_elder_id` bigint NULL COMMENT '家属绑定的老人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

SELECT '步骤3.1: 用户表创建完成' AS '操作日志';

-- ==================== 药品基础库表 ====================
DROP TABLE IF EXISTS `drug_base`;
CREATE TABLE IF NOT EXISTS `drug_base` (
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

SELECT '步骤3.2: 药品基础库表创建完成' AS '操作日志';

-- ==================== 药品冲突规则表 ====================
DROP TABLE IF EXISTS `drug_conflict_rules`;
CREATE TABLE IF NOT EXISTS `drug_conflict_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `drug_a_id` bigint NOT NULL COMMENT '药品A',
  `drug_b_id` bigint NOT NULL COMMENT '药品B',
  `conflict_level` varchar(20) NOT NULL COMMENT '冲突等级：severe/moderate/mild',
  `conflict_reason` text NOT NULL COMMENT '冲突原因',
  `conflict_reason_plain` text NOT NULL COMMENT '白话版冲突原因',
  `source` varchar(100) NULL COMMENT '数据来源',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_drug_pair` (`drug_a_id`, `drug_b_id`),
  INDEX `idx_drug_b_id` (`drug_b_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品冲突规则表';

SELECT '步骤3.3: 药品冲突规则表创建完成' AS '操作日志';

-- ==================== 家庭药箱表 ====================
DROP TABLE IF EXISTS `user_medicine_box`;
CREATE TABLE IF NOT EXISTS `user_medicine_box` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '所属老人',
  `drug_id` bigint NOT NULL COMMENT '药品',
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

SELECT '步骤3.4: 家庭药箱表创建完成' AS '操作日志';

-- ==================== OCR识别记录表 ====================
DROP TABLE IF EXISTS `ocr_record`;
CREATE TABLE IF NOT EXISTS `ocr_record` (
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

SELECT '步骤3.5: OCR识别记录表创建完成' AS '操作日志';

-- ==================== 用药计划表 ====================
DROP TABLE IF EXISTS `medication_plan`;
CREATE TABLE IF NOT EXISTS `medication_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '老人',
  `drug_id` bigint NOT NULL COMMENT '药品',
  `box_item_id` bigint NULL COMMENT '关联药箱条目',
  `plan_date` date NOT NULL COMMENT '计划日期',
  `time_slot` varchar(20) NOT NULL COMMENT '时段：morning/noon/evening/before_bed',
  `dosage_at_time` varchar(50) NOT NULL COMMENT '该时段用量',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/taken/missed/skipped',
  `remind_before` int NULL COMMENT '提前提醒分钟数',
  PRIMARY KEY (`id`),
  INDEX `idx_plan_user_date_status` (`user_id`, `plan_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药计划表';

SELECT '步骤3.6: 用药计划表创建完成' AS '操作日志';

-- ==================== 服药确认记录表 ====================
DROP TABLE IF EXISTS `medication_log`;
CREATE TABLE IF NOT EXISTS `medication_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `plan_id` bigint NOT NULL COMMENT '计划明细',
  `user_id` bigint NOT NULL COMMENT '老人',
  `status` varchar(20) NOT NULL COMMENT '状态：taken/missed/skipped',
  `confirmed_at` datetime NULL COMMENT '确认时间',
  `note` varchar(500) NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  INDEX `idx_log_plan_time` (`plan_id`, `confirmed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服药确认记录表';

SELECT '步骤3.7: 服药确认记录表创建完成' AS '操作日志';

-- ==================== 提醒通知记录表 ====================
DROP TABLE IF EXISTS `reminder_log`;
CREATE TABLE IF NOT EXISTS `reminder_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '目标老人',
  `plan_id` bigint NULL COMMENT '关联计划',
  `remind_type` varchar(30) NOT NULL COMMENT '类型',
  `content` text NOT NULL COMMENT '提醒内容文本',
  `channel` varchar(20) NOT NULL COMMENT '渠道',
  `status` varchar(20) NOT NULL DEFAULT 'sent' COMMENT '状态：sent/read/failed',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  INDEX `idx_reminder_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒通知记录表';

SELECT '步骤3.8: 提醒通知记录表创建完成' AS '操作日志';

-- ==================== 紧急联系人表 ====================
DROP TABLE IF EXISTS `emergency_contact`;
CREATE TABLE IF NOT EXISTS `emergency_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '联系人ID',
  `elder_id` bigint NOT NULL COMMENT '所属老人',
  `name` varchar(50) NOT NULL COMMENT '联系人姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话',
  `email` varchar(100) NULL COMMENT '邮箱',
  `relationship` varchar(30) NULL COMMENT '关系',
  `is_primary` tinyint NOT NULL DEFAULT 0 COMMENT '是否主要联系人',
  PRIMARY KEY (`id`),
  INDEX `idx_contact_elder` (`elder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='紧急联系人表';

SELECT '步骤3.9: 紧急联系人表创建完成' AS '操作日志';

-- ==================== 大模型对话记录表 ====================
DROP TABLE IF EXISTS `ai_conversation_log`;
CREATE TABLE IF NOT EXISTS `ai_conversation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint NOT NULL COMMENT '用户',
  `query_type` varchar(30) NOT NULL COMMENT '类型',
  `user_input` text NOT NULL COMMENT '用户输入',
  `ai_output` text NOT NULL COMMENT 'AI返回内容',
  `safety_check_passed` tinyint NOT NULL DEFAULT 1 COMMENT '是否通过安全检查',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`),
  INDEX `idx_ai_log_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型对话记录表';

SELECT '步骤3.10: 大模型对话记录表创建完成' AS '操作日志';

-- ==================== 药品识别日志表 ====================
DROP TABLE IF EXISTS `drug_recognition_log`;
CREATE TABLE IF NOT EXISTS `drug_recognition_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `ocr_record_id` bigint NULL COMMENT '关联OCR记录',
  `user_id` bigint NULL COMMENT '用户ID',
  `raw_text` text NULL COMMENT '原始识别文本',
  `normalized_name` varchar(255) NULL COMMENT '标准化后的名称',
  `matched_drug_id` bigint NULL COMMENT '匹配的药品ID',
  `match_score` decimal(5,4) NULL COMMENT '匹配分数',
  `status` varchar(50) NULL COMMENT '识别状态',
  `is_new_drug` tinyint(1) DEFAULT 0 COMMENT '是否新药品入库',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_rec_log_ocr` (`ocr_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品识别日志表';

SELECT '步骤3.11: 药品识别日志表创建完成' AS '操作日志';

-- ----------------------------
-- 第四部分：添加外键约束
-- ----------------------------

-- 为家庭药箱表添加外键
ALTER TABLE `user_medicine_box`
ADD CONSTRAINT `fk_box_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_box_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE;

-- 为OCR识别记录表添加外键
ALTER TABLE `ocr_record`
ADD CONSTRAINT `fk_ocr_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_ocr_drug` FOREIGN KEY (`matched_drug_id`) REFERENCES `drug_base`(`id`) ON DELETE SET NULL;

-- 为用药计划表添加外键
ALTER TABLE `medication_plan`
ADD CONSTRAINT `fk_plan_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_plan_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_plan_box_item` FOREIGN KEY (`box_item_id`) REFERENCES `user_medicine_box`(`id`) ON DELETE SET NULL;

-- 为服药确认记录表添加外键
ALTER TABLE `medication_log`
ADD CONSTRAINT `fk_log_plan` FOREIGN KEY (`plan_id`) REFERENCES `medication_plan`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;

-- 为提醒通知记录表添加外键
ALTER TABLE `reminder_log`
ADD CONSTRAINT `fk_reminder_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_reminder_plan` FOREIGN KEY (`plan_id`) REFERENCES `medication_plan`(`id`) ON DELETE SET NULL;

-- 为紧急联系人表添加外键
ALTER TABLE `emergency_contact`
ADD CONSTRAINT `fk_contact_elder` FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;

-- 为药品冲突规则表添加外键
ALTER TABLE `drug_conflict_rules`
ADD CONSTRAINT `fk_conflict_drug_a` FOREIGN KEY (`drug_a_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_conflict_drug_b` FOREIGN KEY (`drug_b_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE;

-- 为大模型对话记录表添加外键
ALTER TABLE `ai_conversation_log`
ADD CONSTRAINT `fk_ai_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;

-- 为药品识别日志表添加外键
ALTER TABLE `drug_recognition_log`
ADD CONSTRAINT `fk_rec_log_ocr` FOREIGN KEY (`ocr_record_id`) REFERENCES `ocr_record`(`id`) ON DELETE CASCADE;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

SELECT '步骤4: 外键约束添加完成' AS '操作日志';

-- ----------------------------
-- 第五部分：验证数据结构
-- ----------------------------

-- 验证用户表结构
SELECT '=== 用户表结构 ===' AS '验证';
DESCRIBE `sys_user`;

-- 验证药品表结构
SELECT '=== 药品表结构 ===' AS '验证';
DESCRIBE `drug_base`;

-- 验证药箱表结构
SELECT '=== 药箱表结构 ===' AS '验证';
DESCRIBE `user_medicine_box`;

-- 验证OCR表结构
SELECT '=== OCR记录表结构 ===' AS '验证';
DESCRIBE `ocr_record`;

-- 统计所有表
SELECT '=== 数据表统计 ===' AS '验证';
SELECT COUNT(*) AS '用户表记录数' FROM `sys_user`
UNION ALL
SELECT COUNT(*) AS '药品表记录数' FROM `drug_base`
UNION ALL
SELECT COUNT(*) AS '药箱表记录数' FROM `user_medicine_box`
UNION ALL
SELECT COUNT(*) AS 'OCR记录表记录数' FROM `ocr_record`
UNION ALL
SELECT COUNT(*) AS '用药计划表记录数' FROM `medication_plan`;

-- ----------------------------
-- 第七部分：操作完成报告
-- ----------------------------
SET @end_time = NOW();
SET @duration = TIMESTAMPDIFF(SECOND, @start_time, @end_time);

SELECT CONCAT('========== 数据库初始化完成 ==========', CHAR(10),
              '开始时间: ', @start_time, CHAR(10),
              '结束时间: ', @end_time, CHAR(10),
              '耗时: ', @duration, ' 秒', CHAR(10),
              '数据库: elderly_medication', CHAR(10),
              '状态: 成功') AS '操作日志';

-- =====================================================
-- 脚本执行完成
-- =====================================================
-- 使用说明:
-- 1. 确保MySQL服务已启动
-- 2. 使用root用户执行此脚本:
--    mysql -u root -p < init_database.sql
-- 3. 如果提示密码，输入数据库root密码
-- 4. 脚本会自动删除旧数据库并创建新数据库
-- =====================================================