package com.example.backend.service.rag;

import com.example.backend.model.dto.RagAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯检索服务（无 LLM 依赖）
 * <p>
 * 职责：给定查询文本，走「向量检索 → 关键词降级」两级检索，返回 Top-K 知识切片。
 * 不含生成逻辑，因此可被 RagService 与 DeepSeekServiceImpl 同时注入，
 * 避免「RagService 依赖 DeepSeek，DeepSeek 又依赖 RagService」的构造器循环依赖。
 * <p>
 * 这也让"检索"成为独立可复用能力：药品补全/冲突检测/今日一课等现有 AI 功能
 * 都能通过它拿到知识依据，再交给 LLM 生成。
 */
@Service
public class RagSearchService {

    private static final Logger logger = LoggerFactory.getLogger(RagSearchService.class);

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final KeywordIndex keywordIndex;

    /** 向量检索 top-1 相似度阈值：低于此值视为不可靠，降级关键词检索 */
    @Value("${ai.rag.vector-score-threshold:0.45}")
    private double vectorScoreThreshold;

    public RagSearchService(EmbeddingService embeddingService,
                            VectorStore vectorStore,
                            KeywordIndex keywordIndex) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.keywordIndex = keywordIndex;
    }

    /**
     * 检索结果：命中模式 + Top-K 切片
     */
    public static final class SearchResult {
        public final String mode;
        public final List<VectorStore.ScoredChunk> hits;

        SearchResult(String mode, List<VectorStore.ScoredChunk> hits) {
            this.mode = mode;
            this.hits = hits;
        }
    }

    /**
     * 检索：向量优先，低置信度/无结果时降级关键词
     * <p>
     * 降级依据不只看「是否为空」，还看 top-1 相似度是否低于阈值
     * （`ai.rag.vector-score-threshold`，默认 0.45）：
     * 向量检索得分普遍偏低说明当前向量质量不可靠（如本地降级哈希向量、知识库膨胀后的碰撞），
     * 此时交给关键词检索——关键词对精确药名/疾病名是强项。
     * 配了 SiliconFlow bge-m3 后语义相似度通常 > 0.5，不会误触发降级。
     *
     * @param query 查询文本（问题 / 药品名 / 慢病名）
     * @param topK  返回条数
     */
    public SearchResult search(String query, int topK) {
        float[] queryVec = embeddingService.embed(query);
        List<VectorStore.ScoredChunk> hits = vectorStore.search(queryVec, topK);
        String mode = RagAnswer.MODE_VECTOR;

        boolean lowConfidence = hits.isEmpty()
                || hits.get(0).score < vectorScoreThreshold;
        if (lowConfidence) {
            logger.info("[RAG] 向量检索置信度低(top-1={})，降级为关键词检索 - query: {}",
                    hits.isEmpty() ? "无结果" : String.format("%.3f", hits.get(0).score), query);
            hits = keywordIndex.search(query, topK);
            mode = RagAnswer.MODE_KEYWORD;
            // 关键词路径同样做低分尾项过滤（标题加权后 top-1 与尾项差距明显，
            // 只保留 top-1 的 60% 以上，防"布洛芬…注意事项"混入只命中通用词的其他药）
            hits = filterLowScores(hits, 0.6, 2.0);
        } else {
            // 向量模式：过滤低分尾项（相对分数过滤）
            // 只要求 top-1 过阈值不够——第 2、3 名可能只有 0.2 分（如降级向量下
            // "药过期了还能吃吗" 的第 2、3 名是右旋糖酐70/田七痛经胶囊 0.19~0.20），
            // 全塞给 LLM 会让"参考资料"出现明显无关的药。保留 top-1 的 70% 以上，
            // 下限不低于阈值 80%，宁缺毋滥。
            hits = filterLowScores(hits, 0.7, vectorScoreThreshold * 0.8);
        }
        return new SearchResult(mode, hits);
    }

    /**
     * 相对分数过滤：只保留与 top-1 相近的高分项，丢弃低分尾项
     *
     * @param hits   检索结果（已按分数降序）
     * @param ratio  top-1 分数保留比例（如 0.7 表示低于 top-1 的 70% 的丢弃）
     * @param minAbs 绝对分数下限
     */
    private List<VectorStore.ScoredChunk> filterLowScores(List<VectorStore.ScoredChunk> hits,
                                                          double ratio, double minAbs) {
        if (hits.size() <= 1) {
            return hits;
        }
        double top = hits.get(0).score;
        double cutoff = Math.max(top * ratio, minAbs);
        List<VectorStore.ScoredChunk> filtered = new ArrayList<>(hits.size());
        for (VectorStore.ScoredChunk h : hits) {
            if (h.score >= cutoff) {
                filtered.add(h);
            }
        }
        if (filtered.isEmpty()) {
            filtered.add(hits.get(0)); // 至少保留 top-1
        }
        logger.info("[RAG] 检索结果低分过滤: {} -> {} 条 (cutoff={})",
                hits.size(), filtered.size(), String.format("%.3f", cutoff));
        return filtered;
    }

    /**
     * 检索并把结果格式化为 prompt 可用的参考资料文本（带 [1][2] 编号）
     * 供现有 AI 功能做「检索增强」注入
     */
    public String formatContext(String query, int topK) {
        SearchResult result = search(query, topK);
        if (result.hits.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("参考资料：\n");
        for (int i = 0; i < result.hits.size(); i++) {
            VectorStore.ScoredChunk hit = result.hits.get(i);
            sb.append("[").append(i + 1).append("] ").append(hit.chunk.getTitle());
            if (hit.chunk.getSourceRef() != null && !hit.chunk.getSourceRef().isEmpty()) {
                sb.append("（来源：").append(hit.chunk.getSourceRef()).append("）");
            }
            sb.append("\n").append(hit.chunk.getContent()).append("\n\n");
        }
        return sb.toString();
    }
}
