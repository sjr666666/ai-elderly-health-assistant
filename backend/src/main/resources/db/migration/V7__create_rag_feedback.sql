-- V7: RAG 回答反馈表
-- 回答质量反馈闭环：前端"这个回答有用吗"👍/👎 落库，供评测集校准与质量监控
CREATE TABLE IF NOT EXISTS rag_feedback (
    id          BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    user_id     BIGINT       NOT NULL COMMENT '反馈用户ID（sys_user 主键）',
    question    VARCHAR(500) NOT NULL COMMENT '用户问题',
    answer      TEXT         NULL COMMENT 'AI 回答全文',
    rating      TINYINT      NOT NULL COMMENT '评分：1=有用（👍），-1=没用（👎）',
    mode        VARCHAR(20)  NULL COMMENT '检索模式（VECTOR/KEYWORD/LOCAL/GUARDED）',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    PRIMARY KEY (id),
    KEY idx_feedback_user (user_id),
    KEY idx_feedback_rating (rating)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='RAG 回答质量反馈';
