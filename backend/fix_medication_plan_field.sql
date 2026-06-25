-- ====================================
-- 修复 medication_plan 表缺少 reminder_stage 字段
-- ====================================

-- 检查并添加 reminder_stage 字段
ALTER TABLE `medication_plan` 
ADD COLUMN IF NOT EXISTS `reminder_stage` varchar(20) NOT NULL DEFAULT 'none' 
COMMENT '提醒阶段: none/pre_remind/due_now/overdue/notify_family' 
AFTER `remind_before`;

-- 验证字段是否添加成功
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'medication_plan' 
  AND COLUMN_NAME = 'reminder_stage';
