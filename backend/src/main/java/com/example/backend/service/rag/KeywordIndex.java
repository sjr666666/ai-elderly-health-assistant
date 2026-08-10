package com.example.backend.service.rag;

import com.example.backend.model.entity.KnowledgeChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地关键词倒排索引（离线降级检索）
 * <p>
 * 当向量检索不可用（如 embedding 服务故障 / 断网）时，退回纯本地关键词匹配。
 * 实现：对每条切片的标题+内容做<b>字符 bigram 切分</b>（中文场景无需分词器，
 * "阿司匹林" → 阿司/司匹/匹林），建立 bigram → 切片ID 的倒排表；
 * 查询时对问题做同样的 bigram 切分，统计命中切片的词频作为打分，取 Top-K。
 * <p>
 * 这也是项目「AI + 本地双引擎」思想在检索层的体现：在线向量检索，
 * 离线关键词检索，与 OCR 三级回退、10 个离线降级场景一脉相承。
 */
@Component
public class KeywordIndex {

    private static final Logger logger = LoggerFactory.getLogger(KeywordIndex.class);

    /** 倒排表：bigram → 出现过的切片ID 列表 */
    private final Map<String, List<Long>> invertedIndex = new ConcurrentHashMap<>();
    /** 切片ID → 切片本体 */
    private final Map<Long, KnowledgeChunk> chunkById = new ConcurrentHashMap<>();

    /**
     * 重建倒排索引
     */
    public void rebuild(List<KnowledgeChunk> chunks) {
        invertedIndex.clear();
        chunkById.clear();
        for (KnowledgeChunk chunk : chunks) {
            chunkById.put(chunk.getId(), chunk);
            Set<String> grams = bigrams(chunk.getTitle() + " " + safe(chunk.getContent()));
            for (String gram : grams) {
                invertedIndex.computeIfAbsent(gram, k -> new ArrayList<>()).add(chunk.getId());
            }
        }
        logger.info("[RAG] 关键词倒排索引重建完成，bigram 词项 {} 个，切片 {} 条", invertedIndex.size(), chunkById.size());
    }

    /**
     * 关键词检索：对 query 切 bigram，命中切片按命中词项数打分
     */
    public List<VectorStore.ScoredChunk> search(String query, int topK) {
        Set<String> queryGrams = bigrams(query);
        if (queryGrams.isEmpty() || chunkById.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Integer> scoreMap = new HashMap<>();
        for (String gram : queryGrams) {
            List<Long> ids = invertedIndex.get(gram);
            if (ids == null) {
                continue;
            }
            for (Long id : ids) {
                scoreMap.merge(id, 1, Integer::sum);
            }
        }
        List<Map.Entry<Long, Integer>> sorted = new ArrayList<>(scoreMap.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<VectorStore.ScoredChunk> results = new ArrayList<>(Math.min(topK, sorted.size()));
        for (Map.Entry<Long, Integer> e : sorted) {
            KnowledgeChunk chunk = chunkById.get(e.getKey());
            if (chunk != null) {
                results.add(new VectorStore.ScoredChunk(chunk, e.getValue()));
            }
            if (results.size() >= topK) {
                break;
            }
        }
        return results;
    }

    /**
     * 字符 bigram 切分（去空白与标点），返回去重后的词项集合
     */
    private Set<String> bigrams(String text) {
        Set<String> grams = new HashSet<>();
        if (text == null) {
            return grams;
        }
        String cleaned = text.toLowerCase().replaceAll("[\\s\\p{Punct}，。！？、；：\\u201C\\u201D\\u2018\\u2019（）《》【】]", "");
        for (int i = 0; i < cleaned.length() - 1; i++) {
            grams.add(cleaned.substring(i, i + 2));
        }
        return grams;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    public int size() {
        return chunkById.size();
    }
}
