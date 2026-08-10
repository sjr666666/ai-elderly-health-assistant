-- V6: knowledge_chunk 增加来源说明列
-- 用于区分知识可信度（如：OCR说明书原文 / 参考整理），回答可溯源
ALTER TABLE knowledge_chunk
    ADD COLUMN source_ref VARCHAR(300) NULL COMMENT '来源说明（如：OCR说明书原文/参考整理）' AFTER keywords;
