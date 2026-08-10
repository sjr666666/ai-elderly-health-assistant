package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * RAG 知识库切片实体
 * 对应数据库表：knowledge_chunk
 * 每行一个知识切片，来源为药品说明书 / 慢病指南 / 用药FAQ
 *
 * @author backend
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    /** 来源类型常量：药品说明书 */
    public static final String SOURCE_TYPE_DRUG = "DRUG";
    /** 来源类型常量：慢病指南 */
    public static final String SOURCE_TYPE_GUIDE = "GUIDE";
    /** 来源类型常量：用药FAQ */
    public static final String SOURCE_TYPE_FAQ = "FAQ";

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 来源类型: DRUG / GUIDE / FAQ
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 关联业务表ID（如 drug_base.id），可为空
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 知识标题（药品名 / 慢病名 / 问题）
     */
    private String title;

    /**
     * 知识切片内容
     */
    private String content;

    /**
     * embedding 向量（JSON 数组文本），落库持久化，重启后重建内存索引
     */
    @TableField("embedding_json")
    private String embeddingJson;

    /**
     * 关键词（空格分隔），供离线降级检索展示
     */
    private String keywords;

    /**
     * 来源说明（如：OCR说明书原文 / 参考整理）
     * 用于区分知识可信度，回答可溯源
     */
    @TableField("source_ref")
    private String sourceRef;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
