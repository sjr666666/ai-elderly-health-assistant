-- ====================================
-- 修复 medication_plan 表缺少 reminder_stage 字段
-- ====================================

-- 方法1：直接添加字段（如果不确定是否存在，先删除再添加）
-- 注意：执行前请备份数据！

-- 步骤1：检查字段是否存在
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '字段已存在'
        ELSE '字段不存在，需要添加'
    END AS status
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'medication_plan' 
  AND COLUMN_NAME = 'reminder_stage';

-- 步骤2：如果字段不存在，执行以下语句添加
ALTER TABLE `medication_plan` 
ADD COLUMN `reminder_stage` varchar(20) NOT NULL DEFAULT 'none' 
COMMENT '提醒阶段: none/pre_remind/due_now/overdue/notify_family' 
AFTER `remind_before`;

-- 验证字段是否添加成功
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'medication_plan' 
  AND COLUMN_NAME = 'reminder_stage';
