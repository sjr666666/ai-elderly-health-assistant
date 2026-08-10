package com.example.backend.service.rag;

import com.example.backend.model.entity.KnowledgeChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 关键词倒排索引检索单测
 * 验证：中文 bigram 倒排检索的命中正确性、Top-K 限制、无匹配行为
 */
class KeywordIndexTest {

    private KeywordIndex index;

    @BeforeEach
    void setUp() {
        index = new KeywordIndex();
        index.rebuild(List.of(
                chunk(1L, "阿司匹林", "阿司匹林是抗血小板药，用于预防心肌梗死和脑梗死。"),
                chunk(2L, "硝苯地平", "硝苯地平是钙通道阻滞剂，用于治疗高血压。"),
                chunk(3L, "阿莫西林", "阿莫西林是青霉素类抗生素，用于抗感染治疗。"),
                // 干扰项：只含"过期"这类通用词，不应被长 query 命中（最低命中过滤）
                chunk(4L, "右旋糖酐70", "本品应避光保存，过期后不得使用。")
        ));
    }

    @Test
    void 精确药名命中对应切片() {
        List<VectorStore.ScoredChunk> hits = index.search("阿司匹林", 3);
        assertFalse(hits.isEmpty(), "应命中阿司匹林切片");
        assertEquals("阿司匹林", hits.get(0).chunk.getTitle());
    }

    @Test
    void 多关键词按命中数排序() {
        // "阿司匹林 硝苯地平" 应优先命中同时含两者词项的切片（阿司匹林切片标题+内容均命中）
        List<VectorStore.ScoredChunk> hits = index.search("阿司匹林 硝苯地平", 3);
        assertEquals(2, hits.size(), "应命中阿司匹林和硝苯地平两个切片");
        assertTrue(hits.get(0).score >= hits.get(1).score, "结果应按分数降序");
    }

    @Test
    void topK限制返回条数() {
        List<VectorStore.ScoredChunk> hits = index.search("阿司匹林 阿莫西林", 1);
        assertEquals(1, hits.size(), "topK=1 时只返回 1 条");
    }

    @Test
    void 无相关词项返回空列表() {
        // 注意：bigram 过短会误撞通用词（如"生素"），此处选与知识内容完全无重叠的查询
        List<VectorStore.ScoredChunk> hits = index.search("香蕉牛奶蛋糕", 3);
        assertTrue(hits.isEmpty(), "无关查询应返回空");
    }

    @Test
    void 只命中单个通用词的切片被过滤() {
        // "过期"是药品知识里的通用词，右旋糖酐70 只命中 1 个 bigram，
        // 不应进入长 query 的结果（防"药过期了还能吃吗"漂移到无关药品）
        List<VectorStore.ScoredChunk> hits = index.search("药过期了还能吃吗", 3);
        for (VectorStore.ScoredChunk h : hits) {
            assertNotEquals("右旋糖酐70", h.chunk.getTitle(), "只命中 1 个通用词项的切片应被最低命中过滤");
        }
    }

    @Test
    void 空索引检索不抛异常() {
        KeywordIndex empty = new KeywordIndex();
        assertTrue(empty.search("阿司匹林", 3).isEmpty());
    }

    private KnowledgeChunk chunk(Long id, String title, String content) {
        return KnowledgeChunk.builder()
                .id(id)
                .title(title)
                .content(content)
                .build();
    }
}
