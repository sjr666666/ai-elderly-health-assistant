package com.example.backend.service.rag;

import com.example.backend.model.dto.RagAnswer;
import com.example.backend.model.entity.KnowledgeChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RagSearchService 降级策略单测
 * 验证：向量检索 top-1 相似度低于阈值时降级关键词检索（知识库膨胀后防漂移）
 */
class RagSearchServiceTest {

    private EmbeddingService embeddingService;
    private VectorStore vectorStore;
    private KeywordIndex keywordIndex;
    private RagSearchService service;

    @BeforeEach
    void setUp() {
        embeddingService = mock(EmbeddingService.class);
        vectorStore = mock(VectorStore.class);
        keywordIndex = mock(KeywordIndex.class);
        service = new RagSearchService(embeddingService, vectorStore, keywordIndex);
        ReflectionTestUtils.setField(service, "vectorScoreThreshold", 0.45);
        when(embeddingService.embed(any(String.class))).thenReturn(new float[]{1f, 0f});
    }

    @Test
    void top1相似度低于阈值时降级为关键词检索() {
        when(vectorStore.search(any(float[].class), eq(3)))
                .thenReturn(List.of(new VectorStore.ScoredChunk(chunk("无关切片"), 0.30)));
        when(keywordIndex.search(any(String.class), eq(3)))
                .thenReturn(List.of(new VectorStore.ScoredChunk(chunk("布洛芬缓释胶囊"), 6)));

        RagSearchService.SearchResult result = service.search("布洛芬缓释胶囊有什么注意事项", 3);

        assertEquals(RagAnswer.MODE_KEYWORD, result.mode);
        assertEquals("布洛芬缓释胶囊", result.hits.get(0).chunk.getTitle());
        verify(keywordIndex).search(any(String.class), eq(3));
    }

    @Test
    void top1相似度高于阈值时保持向量检索() {
        when(vectorStore.search(any(float[].class), eq(3)))
                .thenReturn(List.of(new VectorStore.ScoredChunk(chunk("阿司匹林问答"), 0.60)));

        RagSearchService.SearchResult result = service.search("阿司匹林和降压药能一起吃吗", 3);

        assertEquals(RagAnswer.MODE_VECTOR, result.mode);
        assertEquals("阿司匹林问答", result.hits.get(0).chunk.getTitle());
        verify(keywordIndex, never()).search(any(String.class), any(Integer.class));
    }

    @Test
    void 向量无结果时降级为关键词检索() {
        when(vectorStore.search(any(float[].class), eq(3))).thenReturn(List.of());
        when(keywordIndex.search(any(String.class), eq(3)))
                .thenReturn(List.of(new VectorStore.ScoredChunk(chunk("高血压"), 5)));

        RagSearchService.SearchResult result = service.search("高血压怎么办", 3);

        assertEquals(RagAnswer.MODE_KEYWORD, result.mode);
    }

    @Test
    void 向量模式下低分尾项被过滤() {
        // top-1 = 0.46 刚过阈值 0.45 → 走向量路径；
        // 第 2、3 名 0.20/0.19 远低于 top-1 的 70%（0.322）→ 应被过滤（防无关药混入参考资料）
        when(vectorStore.search(any(float[].class), eq(3)))
                .thenReturn(List.of(
                        new VectorStore.ScoredChunk(chunk("药过期了还能吃吗"), 0.46),
                        new VectorStore.ScoredChunk(chunk("右旋糖酐70"), 0.20),
                        new VectorStore.ScoredChunk(chunk("田七痛经胶囊"), 0.19)));

        RagSearchService.SearchResult result = service.search("药过期了还能吃吗", 3);

        assertEquals(RagAnswer.MODE_VECTOR, result.mode);
        assertEquals(1, result.hits.size(), "低分尾项应被过滤，只留 top-1");
        assertEquals("药过期了还能吃吗", result.hits.get(0).chunk.getTitle());
    }

    @Test
    void 向量模式下高分多项全部保留() {
        // top-1 = 0.55，第 2 名 0.50 在 top-1 的 70%（0.385）以上 → 都保留
        when(vectorStore.search(any(float[].class), eq(3)))
                .thenReturn(List.of(
                        new VectorStore.ScoredChunk(chunk("布洛芬缓释胶囊"), 0.55),
                        new VectorStore.ScoredChunk(chunk("阿莫西林胶囊"), 0.50)));

        RagSearchService.SearchResult result = service.search("布洛芬和阿莫西林能一起吃吗", 3);

        assertEquals(2, result.hits.size(), "相近的高分项应全部保留");
    }

    @Test
    void 关键词模式下只命中通用词的低分项被尾切() {
        // 关键词降级：标题加权后 top-1=18（布洛芬标题 6 bigram×3），
        // 长春胺/秦川通痹只靠内容命中通用词（6/4）→ 低于 top-1 的 60%（10.8）→ 过滤
        when(vectorStore.search(any(float[].class), eq(3)))
                .thenReturn(List.of(new VectorStore.ScoredChunk(chunk("无关"), 0.30)));
        when(keywordIndex.search(any(String.class), eq(3)))
                .thenReturn(List.of(
                        new VectorStore.ScoredChunk(chunk("布洛芬缓释胶囊"), 18),
                        new VectorStore.ScoredChunk(chunk("长春胺"), 6),
                        new VectorStore.ScoredChunk(chunk("秦川通痹胶囊"), 4)));

        RagSearchService.SearchResult result = service.search("布洛芬缓释胶囊有什么注意事项", 3);

        assertEquals(RagAnswer.MODE_KEYWORD, result.mode);
        assertEquals(1, result.hits.size(), "只命中通用词的低分项应被尾切");
        assertEquals("布洛芬缓释胶囊", result.hits.get(0).chunk.getTitle());
    }

    private KnowledgeChunk chunk(String title) {
        return KnowledgeChunk.builder().title(title).build();
    }
}
