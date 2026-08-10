-- =====================================================
-- V1__init_schema.sql - 核心表结构(由 Flyway 管理)
-- =====================================================
-- 来源: 原 init_database.sql + init_guardian_tables.sql + init_weekly_report_table.sql
-- 变更说明:
--   1. 移除了 CREATE DATABASE / USE / SET NAMES(由 Flyway 连接管理)
--   2. 移除了 add_col_if_missing / add_fk_if_not_exists 存储过程
--      (新库由本脚本完整建表;老库通过 flyway baseline 跳过本脚本)
--   3. 所有建表统一 CREATE TABLE IF NOT EXISTS,保证幂等
--   4. 外键统一在脚本末尾 ALTER TABLE ADD CONSTRAINT(仅执行一次)
-- =====================================================

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

-- ==================== 药品基础库表 ====================
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

-- ==================== 家庭药箱表 ====================
CREATE TABLE IF NOT EXISTS `user_medicine_box` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '所属老人',
  `drug_id` bigint NULL COMMENT '药品',
  `dosage` varchar(50) NOT NULL COMMENT '每次用量',
  `frequency` varchar(50) NOT NULL COMMENT '频率',
  `start_date` date NULL COMMENT '开始服用日期',
  `end_date` date NULL COMMENT '预计结束日期',
  `expiry_date` date NULL COMMENT '药品有效期',
  `total_quantity` decimal(12,3) NULL COMMENT '总数量',
  `remaining_quantity` decimal(12,3) NULL COMMENT '剩余数量',
  `note` varchar(500) NULL COMMENT '用户备注',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/stopped',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_status` (`user_id`, `status`),
  INDEX `idx_drug_id` (`drug_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭药箱表';

-- ==================== OCR识别记录表 ====================
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

-- ==================== 药品识别日志表 ====================
CREATE TABLE IF NOT EXISTS `drug_recognition_log` (
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
CREATE TABLE IF NOT EXISTS `ai_conversation_log` (
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
CREATE TABLE IF NOT EXISTS `medication_plan` (
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
CREATE TABLE IF NOT EXISTS `medication_log` (
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
CREATE TABLE IF NOT EXISTS `reminder_log` (
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
CREATE TABLE IF NOT EXISTS `emergency_contact` (
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
CREATE TABLE IF NOT EXISTS `drug_conflict_rules` (
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
  `phone` varchar(100) NOT NULL COMMENT '接收手机号（AES加密存储）',
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

-- ==================== 老人端通知表 ====================
CREATE TABLE IF NOT EXISTS `elder_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
  `elder_id` bigint NOT NULL COMMENT '老人ID，关联sys_user表',
  `notification_type` varchar(50) NOT NULL COMMENT '通知类型：bind_request/system/medication_reminder',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容',
  `extra_data` json NULL COMMENT '附加数据（JSON格式，如绑定通知中的家属信息）',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  `is_handled` tinyint NOT NULL DEFAULT 0 COMMENT '是否已处理：0-未处理 1-已处理',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  INDEX `idx_notif_elder` (`elder_id`),
  INDEX `idx_notif_read` (`is_read`),
  INDEX `idx_notif_type` (`notification_type`),
  INDEX `idx_notif_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老人端通知表';

-- ==================== AI用药周报表 ====================
CREATE TABLE IF NOT EXISTS `medication_weekly_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` varchar(64) NOT NULL UNIQUE COMMENT '报告唯一标识（UUID）',
  `user_id` bigint NOT NULL COMMENT '用户ID（关联sys_user.user_id）',
  `start_date` date NOT NULL COMMENT '周报起始日期',
  `end_date` date NOT NULL COMMENT '周报结束日期',
  `statistics_json` text NULL COMMENT '总体统计数据JSON',
  `ai_summary` text NULL COMMENT 'AI生成的用药总结和建议',
  `full_report_text` longtext NULL COMMENT '完整报告文本（用于截图展示）',
  `total_plans` int NOT NULL DEFAULT 0 COMMENT '总计划数',
  `taken_count` int NOT NULL DEFAULT 0 COMMENT '已服用数',
  `missed_count` int NOT NULL DEFAULT 0 COMMENT '漏服数',
  `skipped_count` int NOT NULL DEFAULT 0 COMMENT '跳过数',
  `compliance_rate` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT '按时服药率（%）',
  `drug_variety_count` int NOT NULL DEFAULT 0 COMMENT '涉及药品种类数',
  `best_time_slot` varchar(50) NULL COMMENT '表现最好的时段',
  `needs_improvement_time_slot` varchar(50) NULL COMMENT '需要改进的时段',
  `missed_drugs_json` text NULL COMMENT '漏服药品列表JSON',
  `daily_summaries_json` longtext NULL COMMENT '每日汇总详情JSON',
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报告生成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_id` (`report_id`),
  INDEX `idx_user_date` (`user_id`, `start_date`, `end_date`),
  INDEX `idx_generated_at` (`generated_at`),
  INDEX `idx_compliance_rate` (`compliance_rate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药周报表';

-- ==================== 外键约束（统一添加，仅执行一次） ====================
-- 老库通过 flyway baseline 跳过本脚本，外键由原初始化脚本已添加
ALTER TABLE `user_medicine_box` ADD CONSTRAINT `fk_box_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `user_medicine_box` ADD CONSTRAINT `fk_box_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE;
ALTER TABLE `ocr_record` ADD CONSTRAINT `fk_ocr_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `ocr_record` ADD CONSTRAINT `fk_ocr_drug` FOREIGN KEY (`matched_drug_id`) REFERENCES `drug_base`(`id`) ON DELETE SET NULL;
ALTER TABLE `drug_recognition_log` ADD CONSTRAINT `fk_rec_log_ocr` FOREIGN KEY (`ocr_record_id`) REFERENCES `ocr_record`(`id`) ON DELETE CASCADE;
ALTER TABLE `medication_plan` ADD CONSTRAINT `fk_plan_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `medication_plan` ADD CONSTRAINT `fk_plan_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE;
ALTER TABLE `medication_log` ADD CONSTRAINT `fk_log_plan` FOREIGN KEY (`plan_id`) REFERENCES `medication_plan`(`id`) ON DELETE CASCADE;
ALTER TABLE `medication_log` ADD CONSTRAINT `fk_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `reminder_log` ADD CONSTRAINT `fk_remind_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `emergency_contact` ADD CONSTRAINT `fk_contact_elder` FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `drug_conflict_rules` ADD CONSTRAINT `fk_conflict_drug_a` FOREIGN KEY (`drug_a_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE;
ALTER TABLE `drug_conflict_rules` ADD CONSTRAINT `fk_conflict_drug_b` FOREIGN KEY (`drug_b_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE;
ALTER TABLE `daily_lesson` ADD CONSTRAINT `fk_daily_lesson_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `guardian_elder_relation` ADD CONSTRAINT `fk_relation_guardian` FOREIGN KEY (`guardian_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `guardian_elder_relation` ADD CONSTRAINT `fk_relation_elder` FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `sms_notification_log` ADD CONSTRAINT `fk_sms_guardian` FOREIGN KEY (`guardian_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `sms_notification_log` ADD CONSTRAINT `fk_sms_elder` FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `emergency_event` ADD CONSTRAINT `fk_event_elder` FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
ALTER TABLE `elder_notification` ADD CONSTRAINT `fk_notif_elder` FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE;
