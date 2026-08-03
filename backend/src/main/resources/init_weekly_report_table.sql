-- =====================================================
-- AI用药周报 - 数据库表初始化脚本
-- =====================================================
-- 版本: 1.0
-- 创建日期: 2026-06-24
-- 说明: 创建用药周报表，用于存储历史周报记录
-- 执行方式: 
--   MySQL 命令行: source /path/to/init_weekly_report_table.sql
--   Navicat:      打开脚本 → 运行
--   DBeaver:      右键脚本 → Execute
-- =====================================================

USE `elderly_medication`;

-- 设置客户端字符集
SET NAMES utf8mb4;

-- 关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- ==================== 用药周报表 ====================
DROP TABLE IF EXISTS `medication_weekly_report`;
CREATE TABLE `medication_weekly_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` varchar(64) NOT NULL UNIQUE COMMENT '报告唯一标识（UUID）',
  `user_id` bigint NOT NULL COMMENT '用户ID（关联sys_user.user_id）',
  `start_date` date NOT NULL COMMENT '周报起始日期',
  `end_date` date NOT NULL COMMENT '周报结束日期',
  
  -- 统计数据（JSON格式存储，便于扩展）
  `statistics_json` text NULL COMMENT '总体统计数据JSON',
  
  -- AI生成内容
  `ai_summary` text NULL COMMENT 'AI生成的用药总结和建议',
  `full_report_text` longtext NULL COMMENT '完整报告文本（用于截图展示）',
  
  -- 关键指标（冗余字段，便于快速查询和统计）
  `total_plans` int NOT NULL DEFAULT 0 COMMENT '总计划数',
  `taken_count` int NOT NULL DEFAULT 0 COMMENT '已服用数',
  `missed_count` int NOT NULL DEFAULT 0 COMMENT '漏服数',
  `skipped_count` int NOT NULL DEFAULT 0 COMMENT '跳过数',
  `compliance_rate` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT '按时服药率（%）',
  `drug_variety_count` int NOT NULL DEFAULT 0 COMMENT '涉及药品种类数',
  
  -- 时段分析
  `best_time_slot` varchar(50) NULL COMMENT '表现最好的时段',
  `needs_improvement_time_slot` varchar(50) NULL COMMENT '需要改进的时段',
  
  -- 漏服药品列表（JSON数组）
  `missed_drugs_json` text NULL COMMENT '漏服药品列表JSON',
  
  -- 每日详情（JSON数组）
  `daily_summaries_json` longtext NULL COMMENT '每日汇总详情JSON',
  
  -- 元数据
  `generated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报告生成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_id` (`report_id`),
  INDEX `idx_user_date` (`user_id`, `start_date`, `end_date`),
  INDEX `idx_generated_at` (`generated_at`),
  INDEX `idx_compliance_rate` (`compliance_rate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用药周报表';

-- ==================== 添加注释说明 ====================
ALTER TABLE `medication_weekly_report` COMMENT = 'AI用药周报表 - 存储用户每周用药情况统计报告和AI建议';

-- 重新开启外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ==================== 验证表创建 ====================
SELECT 
  TABLE_NAME AS '表名',
  TABLE_COMMENT AS '表注释',
  CREATE_TIME AS '创建时间'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'elderly_medication'
  AND TABLE_NAME = 'medication_weekly_report';

-- ==================== 显示表结构 ====================
DESCRIBE `medication_weekly_report`;

-- ==================== 示例数据（可选，用于测试） ====================
-- 取消注释以下代码可插入测试数据
/*
INSERT INTO `medication_weekly_report` (
  `report_id`, `user_id`, `start_date`, `end_date`,
  `statistics_json`, `ai_summary`, `full_report_text`,
  `total_plans`, `taken_count`, `missed_count`, `skipped_count`,
  `compliance_rate`, `drug_variety_count`,
  `best_time_slot`, `needs_improvement_time_slot`,
  `missed_drugs_json`, `daily_summaries_json`
) VALUES (
  UUID(),
  123456789,
  '2026-06-17',
  '2026-06-24',
  '{"totalPlans":28,"takenCount":24,"missedCount":3,"skippedCount":1,"pendingCount":0,"complianceRate":89.29,"drugVarietyCount":4}',
  '【优秀】本周用药依从性非常好！继续保持规律服药的习惯。\n\n坚持规律服药是控制病情的关键，祝您健康！',
  '========================================\n       AI用药周报\n========================================\n\n📊 总体统计\n----------------------------------------\n总计划数：28次\n已服用：24次\n漏服：3次\n跳过：1次\n按时服药率：89.3%\n药品种类：4种\n\n⏰ 时段分析\n----------------------------------------\n表现最好：早上\n需改进：晚上\n\n💡 AI建议\n----------------------------------------\n【优秀】本周用药依从性非常好！继续保持规律服药的习惯。\n\n坚持规律服药是控制病情的关键，祝您健康！\n\n========================================\n数据来源：AI药管家\n生成时间：2026-06-24 10:30:00\n========================================',
  28, 24, 3, 1,
  89.29, 4,
  '早上', '晚上',
  '["降压药"]',
  '[{"date":"2026-06-17","dayOfWeek":"周三","totalPlans":4,"takenCount":4,"missedCount":0,"complianceRate":100.0,"drugs":["阿司匹林","二甲双胍"]}]'
);
*/

-- ==================== 完成提示 ====================
SELECT '✅ 用药周报表 medication_weekly_report 创建成功！' AS '执行结果';
