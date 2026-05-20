-- =====================================================
-- 老年人用药管理系统 - 数据库初始化脚本
-- =====================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `elderly_medication` 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `elderly_medication`;

-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- ==================== 用户表 ====================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
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

-- =============== 添加外键约束 ===============
ALTER TABLE `user_medicine_box`
ADD CONSTRAINT `fk_box_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_box_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE;

ALTER TABLE `ocr_record`
ADD CONSTRAINT `fk_ocr_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_ocr_drug` FOREIGN KEY (`matched_drug_id`) REFERENCES `drug_base`(`id`) ON DELETE SET NULL;

ALTER TABLE `drug_recognition_log`
ADD CONSTRAINT `fk_rec_log_ocr` FOREIGN KEY (`ocr_record_id`) REFERENCES `ocr_record`(`id`) ON DELETE CASCADE;

ALTER TABLE `medication_plan`
ADD CONSTRAINT `fk_plan_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_plan_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE;

ALTER TABLE `medication_log`
ADD CONSTRAINT `fk_log_plan` FOREIGN KEY (`plan_id`) REFERENCES `medication_plan`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;

ALTER TABLE `reminder_log`
ADD CONSTRAINT `fk_remind_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;

ALTER TABLE `emergency_contact`
ADD CONSTRAINT `fk_contact_elder` FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;

ALTER TABLE `drug_conflict_rules`
ADD CONSTRAINT `fk_conflict_drug_a` FOREIGN KEY (`drug_a_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE,
ADD CONSTRAINT `fk_conflict_drug_b` FOREIGN KEY (`drug_b_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- =============== 插入测试数据 ===============
INSERT INTO `sys_user` (`user_id`, `username`, `password`, `real_name`, `age`, `allergy_history`, `chronic_diseases`, `role`) VALUES
(10001, 'laowang', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '王阿姨', 68, '无药物过敏史', '高血压、糖尿病', 'elder'),
(10002, 'zhangsan', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '张三', 35, NULL, NULL, 'family');

INSERT INTO `drug_base` (`approval_number`, `generic_name`, `trade_name`, `specification`, `manufacturer`, `category`, `description`) VALUES
('国药准字Z44021856', '感冒灵颗粒', '999', '每袋装10克', '华润三九医药股份有限公司', '中成药', '【成分】三叉苦、金盏银盘、野菊花、岗梅、咖啡因、对乙酰氨基酚、马来酸氯苯那敏。【适应症】用于感冒引起的头痛，发热，鼻塞，流涕，咽痛等。'),
('国药准字H10910052', '硝苯地平缓释片', '伲福达', '10mg×30片', '青岛黄海制药有限责任公司', '化学药品', '【成分】硝苯地平。【适应症】用于治疗高血压、心绞痛。'),
('国药准字H11021309', '阿司匹林肠溶片', '拜阿司匹林', '100mg×30片', '拜耳医药保健有限公司', '化学药品', '【成分】阿司匹林。【适应症】用于抑制血小板聚集。'),
('国药准字H44021524', '阿莫西林胶囊', '阿莫仙', '0.5g×24粒', '珠海联邦制药股份有限公司', '化学药品', '【成分】阿莫西林。【适应症】用于敏感菌所致的呼吸道、泌尿生殖道感染。'),
('国药准字Z44023485', '板蓝根颗粒', '白云山', '每袋装10克', '广州白云山和记黄埔中药有限公司', '中成药', '【成分】板蓝根。【适应症】清热解毒，凉血，利咽。'),
('国药准字H10970418', '氯雷他定片', '开瑞坦', '10mg×6片', '上海先灵葆雅制药有限公司', '化学药品', '【成分】氯雷他定。【适应症】用于缓解过敏性鼻炎症状。'),
('国药准字Z11020377', '藿香正气水', '同仁堂', '每支装10毫升', '北京同仁堂科技发展股份有限公司制药厂', '中成药', '【成分】苍术、陈皮、厚朴等。【适应症】解表化湿，理气和中。'),
('国药准字H11021600', '葡萄糖酸钙片', '双鹤', '0.5g×100片', '北京双鹤药业股份有限公司', '化学药品', '【成分】葡萄糖酸钙。【适应症】用于预防和治疗钙缺乏症。');

-- 输出完成信息
SELECT '数据库初始化完成！' AS result;
SELECT COUNT(*) AS table_count FROM information_schema.TABLES WHERE TABLE_SCHEMA='elderly_medication' AND TABLE_TYPE='BASE TABLE';
