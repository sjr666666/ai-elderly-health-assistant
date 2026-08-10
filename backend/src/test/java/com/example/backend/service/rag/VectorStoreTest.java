package com.example.backend.service.rag;

import com.example.backend.model.entity.KnowledgeChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 内存向量索引检索单测
 * 验证：L2 归一化向量点积（余弦相似度）的 Top-K 排序正确性
 */
class VectorStoreTest {

    private VectorStore store;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        store = new VectorStore(objectMapper);
        // 三个归一化向量：A 偏向 x 轴，B 偏向 y 轴，C 偏向 z 轴
        store.rebuild(List.of(
                chunk(1L, "降压药知识", new float[]{1f, 0, 0}),
                chunk(2L, "降糖药知识", new float[]{0, 1f, 0}),
                chunk(3L, "感冒药知识", new float[]{0, 0, 1f})
        ));
    }

    @Test
    void 与查询方向一致的切片排第一() {
        // 查询向量接近 x 轴 → 应命中 A（降压药知识）
        List<VectorStore.ScoredChunk> hits = store.search(new float[]{0.9f, 0.1f, 0f}, 3);
        assertFalse(hits.isEmpty());
        assertEquals(1L, hits.get(0).chunk.getId(), "最相似切片应排第一");
        assertEquals(3, hits.size(), "默认返回全部 3 条");
    }

    @Test
    void topK限制返回数量() {
        List<VectorStore.ScoredChunk> hits = store.search(new float[]{1f, 0f, 0f}, 2);
        assertEquals(2, hits.size(), "topK=2 只返回 2 条");
    }

    @Test
    void 相似度分数单调递减() {
        List<VectorStore.ScoredChunk> hits = store.search(new float[]{0.9f, 0.05f, 0.05f}, 3);
        for (int i = 1; i < hits.size(); i++) {
            assertTrue(hits.get(i - 1).score >= hits.get(i).score, "分数应单调递减");
        }
    }

    @Test
    void 空索引检索不抛异常() {
        VectorStore empty = new VectorStore(objectMapper);
        assertTrue(empty.search(new float[]{1f, 0f, 0f}, 3).isEmpty());
    }

    private KnowledgeChunk chunk(Long id, String title, float[] vec) throws Exception {
        return KnowledgeChunk.builder()
                .id(id)
                .title(title)
                .content(title)
                .embeddingJson(objectMapper.writeValueAsString(vec))
                .build();
    }
}
