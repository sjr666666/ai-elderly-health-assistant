-- ----------------------------
-- 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL UNIQUE COMMENT '登录名',
  `password` varchar(255) NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名/称呼',
  `age` tinyint NULL COMMENT '年龄',
  `allergy_history` text NULL COMMENT '过敏史描述（如“青霉素过敏”）',
  `chronic_diseases` text NULL COMMENT '慢性病史描述（如“高血压、糖尿病”）',
  `role` varchar(20) NOT NULL DEFAULT 'elder' COMMENT '角色：elder（老人）/ family（家属）',
  `bind_elder_id` bigint NULL COMMENT '家属绑定的老人ID（自关联）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 药品基础库表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `drug_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  `approval_number` varchar(100) UNIQUE COMMENT '国药准字',
  `generic_name` varchar(200) NOT NULL COMMENT '通用名（化学名）',
  `trade_name` varchar(200) NULL COMMENT '商品名（如“开博通”）',
  `common_name` varchar(200) NULL COMMENT '俗名/别名（如“降压0号”）',
  `specification` varchar(100) NULL COMMENT '规格（如“5mg*30片”）',
  `manufacturer` varchar(200) NULL COMMENT '生产厂家',
  `category` varchar(100) NULL COMMENT '药品分类（处方药/非处方药/保健品）',
  `description` text NULL COMMENT '药品说明原文',
  `image_url` varchar(500) NULL COMMENT '药品标准图片',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_approval_number` (`approval_number`),
  INDEX `idx_generic_name` (`generic_name`),
  FULLTEXT KEY `ft_common_generic` (`common_name`, `generic_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品基础库表';

-- ----------------------------
-- 药品冲突规则表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `drug_conflict_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `drug_a_id` bigint NOT NULL COMMENT '药品A',
  `drug_b_id` bigint NOT NULL COMMENT '药品B',
  `conflict_level` varchar(20) NOT NULL COMMENT '冲突等级：severe/moderate/mild',
  `conflict_reason` text NOT NULL COMMENT '冲突原因（专业描述）',
  `conflict_reason_plain` text NOT NULL COMMENT '白话版冲突原因（展示给老人）',
  `source` varchar(100) NULL COMMENT '数据来源',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_drug_pair` (`drug_a_id`, `drug_b_id`),
  INDEX `idx_drug_b_id` (`drug_b_id`),
  CONSTRAINT `fk_conflict_drug_a` FOREIGN KEY (`drug_a_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conflict_drug_b` FOREIGN KEY (`drug_b_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品冲突规则表';

-- ----------------------------
-- 家庭药箱表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_medicine_box` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '所属老人',
  `drug_id` bigint NOT NULL COMMENT '药品',
  `dosage` varchar(50) NOT NULL COMMENT '每次用量（如“一片”“半片”）',
  `frequency` varchar(50) NOT NULL COMMENT '频率（如“每日两次”）',
  `start_date` date NULL COMMENT '开始服用日期',
  `end_date` date NULL COMMENT '预计结束日期',
  `expiry_date` date NULL COMMENT '药品有效期',
  `note` varchar(500) NULL COMMENT '用户备注',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/stopped',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_status` (`user_id`, `status`),
  INDEX `idx_drug_id` (`drug_id`),
  CONSTRAINT `fk_box_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_box_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭药箱表';

-- ----------------------------
-- OCR识别记录表
-- ----------------------------
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
  CONSTRAINT `fk_ocr_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ocr_drug` FOREIGN KEY (`matched_drug_id`) REFERENCES `drug_base`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OCR识别记录表';

-- ----------------------------
-- 用药计划表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `medication_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id` bigint NOT NULL COMMENT '老人',
  `drug_id` bigint NOT NULL COMMENT '药品',
  `box_item_id` bigint NULL COMMENT '关联药箱条目',
  `plan_date` date NOT NULL COMMENT '计划日期',
  `time_slot` varchar(20) NOT NULL COMMENT '时段：morning/noon/evening/before_bed',
  `dosage_at_time` varchar(50) NOT NULL COMMENT '该时段用量（如“1片”）',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/taken/missed/skipped',
  `remind_before` int NULL COMMENT '提前提醒分钟数（如15）',
  PRIMARY KEY (`id`),
  INDEX `idx_plan_user_date_status` (`user_id`, `plan_date`, `status`),
  CONSTRAINT `fk_plan_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_drug` FOREIGN KEY (`drug_id`) REFERENCES `drug_base`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_box_item` FOREIGN KEY (`box_item_id`) REFERENCES `user_medicine_box`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药计划表';

-- ----------------------------
-- 服药确认记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `medication_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `plan_id` bigint NOT NULL COMMENT '计划明细',
  `user_id` bigint NOT NULL COMMENT '老人',
  `status` varchar(20) NOT NULL COMMENT '状态：taken/missed/skipped',
  `confirmed_at` datetime NULL COMMENT '确认时间',
  `note` varchar(500) NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  INDEX `idx_log_plan_time` (`plan_id`, `confirmed_at`),
  CONSTRAINT `fk_log_plan` FOREIGN KEY (`plan_id`) REFERENCES `medication_plan`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服药确认记录表';

-- ----------------------------
-- 提醒通知记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `reminder_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '目标老人',
  `plan_id` bigint NULL COMMENT '关联计划（可为空表示非周期提醒）',
  `remind_type` varchar(30) NOT NULL COMMENT '类型：dosage_remind/expiry_warning/missed_alert',
  `content` text NOT NULL COMMENT '提醒内容文本',
  `channel` varchar(20) NOT NULL COMMENT '渠道：browser_notify/page_popup/email/sms',
  `status` varchar(20) NOT NULL DEFAULT 'sent' COMMENT '状态：sent/read/failed',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  INDEX `idx_reminder_user_time` (`user_id`, `created_at`),
  CONSTRAINT `fk_reminder_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reminder_plan` FOREIGN KEY (`plan_id`) REFERENCES `medication_plan`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒通知记录表';

-- ----------------------------
-- 紧急联系人表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `emergency_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '联系人ID',
  `elder_id` bigint NOT NULL COMMENT '所属老人',
  `name` varchar(50) NOT NULL COMMENT '联系人姓名',
  `phone` varchar(20) NOT NULL COMMENT '联系电话（用于tel:协议）',
  `email` varchar(100) NULL COMMENT '邮箱（通知用）',
  `relationship` varchar(30) NULL COMMENT '关系（子女、护工等）',
  `is_primary` tinyint NOT NULL DEFAULT 0 COMMENT '是否主要联系人',
  PRIMARY KEY (`id`),
  INDEX `idx_contact_elder` (`elder_id`),
  CONSTRAINT `fk_contact_elder` FOREIGN KEY (`elder_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='紧急联系人表';

-- ----------------------------
-- 大模型对话记录表（可选）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_conversation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint NOT NULL COMMENT '用户',
  `query_type` varchar(30) NOT NULL COMMENT '类型：explain/conflict_check/emergency',
  `user_input` text NOT NULL COMMENT '用户输入（文本或识别结果）',
  `ai_output` text NOT NULL COMMENT 'AI返回白话内容',
  `safety_check_passed` tinyint NOT NULL DEFAULT 1 COMMENT '是否通过安全检查',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`),
  INDEX `idx_ai_log_user_time` (`user_id`, `created_at`),
  CONSTRAINT `fk_ai_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型对话记录表';
