-- V5: RAG 用药知识库切片表
-- 每行 = 一个知识切片（来自药品说明书 / 慢病指南 / 用药FAQ）
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    source_type     VARCHAR(32)  NOT NULL COMMENT '来源类型: DRUG(药品说明书) / GUIDE(慢病指南) / FAQ(用药问答)',
    source_id       BIGINT       NULL COMMENT '关联业务表ID（如 drug_base.id）',
    title           VARCHAR(200) NOT NULL COMMENT '知识标题（如药品名/慢病名/问题）',
    content         TEXT         NOT NULL COMMENT '知识切片内容',
    embedding_json  TEXT         NULL COMMENT 'embedding 向量（JSON 数组，落库持久化，重启后重建内存索引）',
    keywords        VARCHAR(500) NULL COMMENT '关键词（空格分隔，供离线降级检索展示）',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG知识库切片表';
