package com.example.backend.service.rag;

import com.example.backend.model.entity.KnowledgeChunk;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内存向量索引（在线检索）
 * <p>
 * 知识量级为百~千条时，全量遍历 + 点积计算完全够用（毫秒级），无需引入 FAISS/Milvus 等重型组件，
 * 保持了项目 MySQL + Redis 的轻量架构。索引在入库后重建，启动时从 knowledge_chunk 表加载。
 * <p>
 * 检索原理：Embedding 已做 L2 归一化，因此点积(query, chunk) 即余弦相似度，
 * 取 Top-K 即语义最相近的知识切片。
 */
@Component
public class VectorStore {

    private static final Logger logger = LoggerFactory.getLogger(VectorStore.class);

    private final ObjectMapper objectMapper;

    /** 索引项：知识切片 + 其向量 */
    private static final class Entry {
        final KnowledgeChunk chunk;
        final float[] vector;
        Entry(KnowledgeChunk chunk, float[] vector) {
            this.chunk = chunk;
            this.vector = vector;
        }
    }

    /** 检索结果项 */
    public static final class ScoredChunk {
        public final KnowledgeChunk chunk;
        public final double score;
        ScoredChunk(KnowledgeChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }

    private volatile List<Entry> index = Collections.emptyList();
    private final AtomicInteger size = new AtomicInteger(0);

    public VectorStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 重建索引：将 embeddingJson 反序列化为向量后放入内存
     */
    public void rebuild(List<KnowledgeChunk> chunks) {
        List<Entry> newIndex = new ArrayList<>(chunks.size());
        for (KnowledgeChunk chunk : chunks) {
            if (chunk.getEmbeddingJson() == null || chunk.getEmbeddingJson().isEmpty()) {
                continue;
            }
            try {
                float[] vec = objectMapper.readValue(chunk.getEmbeddingJson(),
                        new TypeReference<float[]>() {});
                newIndex.add(new Entry(chunk, vec));
            } catch (Exception e) {
                logger.warn("[RAG] 切片 {} 向量解析失败，跳过 - {}", chunk.getId(), e.getMessage());
            }
        }
        this.index = newIndex;
        this.size.set(newIndex.size());
        logger.info("[RAG] 向量索引重建完成，共 {} 条切片", newIndex.size());
    }

    /**
     * 语义检索：返回与 query 最相似的 topK 条知识切片
     */
    public List<ScoredChunk> search(float[] queryVec, int topK) {
        if (queryVec == null || index.isEmpty()) {
            return Collections.emptyList();
        }
        List<ScoredChunk> results = new ArrayList<>(index.size());
        for (Entry entry : index) {
            double score = dot(queryVec, entry.vector);
            results.add(new ScoredChunk(entry.chunk, score));
        }
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results.subList(0, Math.min(topK, results.size()));
    }

    /**
     * 点积：归一化向量的点积即余弦相似度，值域 [-1, 1]
     */
    private double dot(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    public int size() {
        return size.get();
    }
}
