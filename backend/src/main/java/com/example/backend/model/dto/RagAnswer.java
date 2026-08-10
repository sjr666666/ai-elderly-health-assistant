package com.example.backend.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * RAG 问答结果 DTO
 * answer   ：LLM 基于检索资料生成的回答（含 [1][2] 引用标注）
 * sources  ：被引用的知识切片（标题/来源/内容片段），前端可展示"根据 XX 说明书"
 * mode     ：本次回答使用的检索模式（VECTOR 向量 / KEYWORD 关键词降级 / LOCAL 本地直出）
 * userDrugs：用户药箱当前服用的药品（个性化上下文，空列表表示未个性化）
 */
@Data
@Builder
public class RagAnswer {

    public static final String MODE_VECTOR = "VECTOR";
    public static final String MODE_KEYWORD = "KEYWORD";
    public static final String MODE_LOCAL = "LOCAL";

    private String answer;
    private String mode;
    private List<Source> sources;
    private List<String> userDrugs;

    /**
     * 引用来源
     */
    @Data
    @Builder
    public static class Source {
        private String title;
        private String sourceType;
        private String content;
        private String sourceRef;
        private double score;
    }
}
