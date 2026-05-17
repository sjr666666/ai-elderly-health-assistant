-- =====================================================
-- OCR 药品识别功能 数据库初始化脚本
-- 适用于 elderly_medication 数据库
-- 创建时间: 2026-05-18
-- 描述: 包含 OCR 识别记录表的创建和相关测试数据
-- =====================================================

-- 使用数据库
USE elderly_medication;

-- =====================================================
-- 第一部分：创建 OCR 识别记录表
-- =====================================================
-- 该表用于存储用户上传药品图片后的 OCR 识别结果

DROP TABLE IF EXISTS `ocr_record`;

CREATE TABLE IF NOT EXISTS `ocr_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID，自增主键',
  `user_id` bigint NOT NULL COMMENT '上传用户ID，关联 sys_user 表',
  `image_url` varchar(500) NOT NULL COMMENT '图片存储路径',
  `raw_text` text NULL COMMENT 'OCR 原始识别文本',
  `matched_drug_id` bigint NULL COMMENT '匹配到的药品ID，关联 drug_base 表',
  `match_score` decimal(5,4) NULL COMMENT '匹配置信度，范围 0-1，保留4位小数',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending（待处理）/ matched（已匹配）/ unmatched（未匹配）/ failed（识别失败）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  INDEX `idx_ocr_user_time` (`user_id`, `created_at`),
  INDEX `idx_ocr_status` (`status`),
  CONSTRAINT `fk_ocr_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ocr_drug` FOREIGN KEY (`matched_drug_id`) REFERENCES `drug_base`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OCR识别记录表';

-- =====================================================
-- 第二部分：添加 OCR 相关索引（优化查询性能）
-- =====================================================

-- 为 OCR 记录表添加复合索引，加速按用户和时间范围查询
ALTER TABLE `ocr_record`
ADD INDEX `idx_ocr_user_status_time` (`user_id`, `status`, `created_at`);

-- 为匹配度查询添加索引（用于相似度匹配场景）
ALTER TABLE `ocr_record`
ADD INDEX `idx_ocr_match_score` (`match_score`);

-- =====================================================
-- 第三部分：OCR 状态枚举值说明
-- =====================================================
-- pending   - 待处理：图片已上传，等待 OCR 引擎识别
-- matched   - 已匹配：OCR 识别成功，且与药品库匹配成功
-- unmatched - 未匹配：OCR 识别成功，但未在药品库中找到匹配
-- failed    - 识别失败：OCR 引擎识别失败或发生错误

-- =====================================================
-- 第四部分：OCR 测试数据
-- =====================================================

-- 注意：在插入 OCR 测试数据前，请确保 sys_user 表中已有测试用户
-- 以下测试数据假设存在 user_id 为 1 的测试用户

-- 插入 OCR 测试记录
INSERT INTO `ocr_record` (`user_id`, `image_url`, `raw_text`, `matched_drug_id`, `match_score`, `status`, `created_at`) VALUES
(1, '/uploads/test_drug_001.webp', '硝苯地平缓释片 5mg×30片 德州德药制药厂', 1001, 0.9523, 'matched', NOW() - INTERVAL 5 DAY),
(1, '/uploads/test_drug_002.webp', '二甲双胍片 0.5g×20片 上海现代制药厂', 1002, 0.9341, 'matched', NOW() - INTERVAL 3 DAY),
(1, '/uploads/test_drug_003.webp', '阿司匹林肠溶片 100mg×30片 拜耳医药', 1003, 0.9156, 'matched', NOW() - INTERVAL 2 DAY),
(1, '/uploads/test_drug_004.webp', '未知药品 XYZ', NULL, NULL, 'unmatched', NOW() - INTERVAL 1 DAY),
(1, '/uploads/test_drug_005.webp', '', NULL, NULL, 'failed', NOW());

-- =====================================================
-- 第五部分：验证 OCR 表结构和数据
-- =====================================================

-- 查看 OCR 记录表结构
DESCRIBE `ocr_record`;

-- 查看 OCR 记录统计
SELECT
    COUNT(*) AS '总记录数',
    SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) AS '待处理',
    SUM(CASE WHEN status = 'matched' THEN 1 ELSE 0 END) AS '已匹配',
    SUM(CASE WHEN status = 'unmatched' THEN 1 ELSE 0 END) AS '未匹配',
    SUM(CASE WHEN status = 'failed' THEN 1 ELSE 0 END) AS '识别失败'
FROM `ocr_record`;

-- 查看最近的 OCR 识别记录
SELECT
    o.id AS '记录ID',
    o.user_id AS '用户ID',
    o.image_url AS '图片路径',
    o.raw_text AS '识别文本',
    d.generic_name AS '匹配药品',
    o.match_score AS '置信度',
    o.status AS '状态',
    o.created_at AS '创建时间'
FROM `ocr_record` o
LEFT JOIN `drug_base` d ON o.matched_drug_id = d.id
ORDER BY o.created_at DESC
LIMIT 10;

-- =====================================================
-- 第六部分：OCR 功能相关表关系说明
-- =====================================================
--
-- sys_user (用户表)
--    │
--    └── 1:N ──→ ocr_record (OCR识别记录表)
--
-- drug_base (药品基础库)
--    │
--    └── 1:N ──→ ocr_record (OCR识别记录表)
--
-- 表关系说明：
-- 1. 一个用户可以多次使用 OCR 识别功能
-- 2. 一条 OCR 记录最多匹配到一个药品（matched_drug_id）
-- 3. 如果未匹配到药品，matched_drug_id 为 NULL
-- 4. 删除用户时，该用户的所有 OCR 记录也会被删除（CASCADE）
-- 5. 删除药品时，OCR 记录中的 matched_drug_id 会设置为 NULL（SET NULL）

-- =====================================================
-- 第七部分：OCR 功能性能优化建议
-- =====================================================
--
-- 1. 定期清理旧的 OCR 记录（如保留30天）
-- 2. 对经常查询的字段添加索引
-- 3. 图片文件建议使用对象存储（如 MinIO、阿里云OSS）
-- 4. 对于高频场景，可以考虑添加 Redis 缓存

-- =====================================================
-- 脚本执行完成
-- =====================================================
