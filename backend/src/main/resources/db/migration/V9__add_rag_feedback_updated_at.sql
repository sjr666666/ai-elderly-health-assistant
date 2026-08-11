-- V9: rag_feedback 补充 updated_at 列
-- 原因：RagFeedback 继承 BaseEntity（含 updated_at，MyBatis-Plus INSERT 自动填充），
-- V7 建表时遗漏该列，导致 insert 报 Unknown column 'updated_at'
ALTER TABLE rag_feedback
    ADD COLUMN updated_at DATETIME NULL COMMENT '更新时间' AFTER created_at;
