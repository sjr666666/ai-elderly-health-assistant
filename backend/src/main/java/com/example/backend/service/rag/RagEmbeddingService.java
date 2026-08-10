package com.example.backend.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 药品知识库 Embedding 服务实现
 * <p>
 * 双策略（可切换、可降级）：
 * <ol>
 *   <li><b>siliconflow</b>：配置了 ai.embedding.api-key 时，调用 SiliconFlow 的 OpenAI 兼容
 *       /v1/embeddings 接口，使用 BAAI/bge-m3 中文向量模型（1024 维）</li>
 *   <li><b>local-hash</b>：未配置 Key 时，退化为内置的字符 bigram 哈希特征向量（512 维），
 *       保证无外部依赖也能跑通"检索→增强→生成"全链路，便于演示与测试</li>
 * </ol>
 * 无论哪种策略，输出向量都会做 L2 归一化，检索时用点积即等价于余弦相似度。
 */
@Service
public class RagEmbeddingService implements EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(RagEmbeddingService.class);

    /** 本地降级向量的固定维度 */
    private static final int FALLBACK_DIM = 512;

    @Value("${ai.embedding.api-url:https://api.siliconflow.cn/v1/embeddings}")
    private String apiUrl;

    @Value("${ai.embedding.api-key:}")
    private String apiKey;

    @Value("${ai.embedding.model:BAAI/bge-m3}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 已配置 provider 标识，@PostConstruct 时决定，全程固定，避免两种向量混用 */
    private String provider;

    /** 简单结果缓存（同一文本重复向量化直接命中，如重复提问场景） */
    private final Map<String, float[]> cache = new ConcurrentHashMap<>();

    public RagEmbeddingService(@Qualifier("aiRestTemplate") RestTemplate restTemplate,
                               ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据是否配置 Key 决定向量化策略
     */
    private String decideProvider() {
        return StringUtils.hasText(apiKey) ? "siliconflow-bge-m3" : "local-hash-fallback";
    }

    /**
     * 启动初始化：决定向量化策略 + 打日志
     * <p>
     * 注意：必须在 @PostConstruct（而非构造器）里决定 provider——构造器执行时 @Value 字段
     * 尚未注入（Spring 字段注入在构造器之后），构造器里读 apiKey 恒为 null，
     * 会导致配了 Key 仍走 local-hash 降级。
     */
    @PostConstruct
    public void init() {
        this.provider = decideProvider();
        if ("local-hash-fallback".equals(provider)) {
            logger.warn("[RAG] 未配置 ai.embedding.api-key，Embedding 使用本地哈希降级方案（{}维），"
                    + "检索精度有限。配置 SiliconFlow Key 可切换为 bge-m3（{}维）。", FALLBACK_DIM, "1024");
        } else {
            logger.info("[RAG] Embedding 提供商: {}，模型: {}，维度: {}", provider, model, "1024");
        }
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            text = " ";
        }
        String key = text.trim();
        return cache.computeIfAbsent(key, this::embedUncached);
    }

    private float[] embedUncached(String text) {
        if ("local-hash-fallback".equals(provider)) {
            return localHashEmbedding(text);
        }
        try {
            return siliconflowEmbedding(text);
        } catch (Exception e) {
            logger.error("[RAG] SiliconFlow embedding 调用失败，降级为本地哈希向量 - {}", e.getMessage());
            return localHashEmbedding(text);
        }
    }

    /**
     * 调用 SiliconFlow OpenAI 兼容 embedding 接口
     */
    private float[] siliconflowEmbedding(String text) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", text);
        requestBody.put("encoding_format", "float");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new IllegalStateException("embedding API status=" + response.getStatusCode());
        }
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode data = root.path("data");
        if (!data.isArray() || data.size() == 0) {
            throw new IllegalStateException("embedding API 响应缺少 data");
        }
        JsonNode emb = data.get(0).path("embedding");
        float[] vec = new float[emb.size()];
        for (int i = 0; i < emb.size(); i++) {
            vec[i] = (float) emb.get(i).asDouble();
        }
        return normalize(vec);
    }

    /**
     * 内置降级向量：字符 bigram 哈希特征（词袋思想）
     * 中文按相邻字符二元组切分（"阿司匹林" → 阿司/司匹/匹林），每个 bigram 哈希到
     * 固定维度桶累加计数，最后 L2 归一化。语义相近的文本在 bigram 层有重叠，可作粗粒度检索。
     */
    private float[] localHashEmbedding(String text) {
        float[] vec = new float[FALLBACK_DIM];
        String cleaned = text.toLowerCase().replaceAll("[\\s\\p{Punct}，。！？、；：\\u201C\\u201D\\u2018\\u2019（）《》【】]", "");
        for (int i = 0; i < cleaned.length() - 1; i++) {
            String bigram = cleaned.substring(i, i + 2);
            int bucket = Math.floorMod(bigram.hashCode(), FALLBACK_DIM);
            vec[bucket] += 1.0f;
        }
        return normalize(vec);
    }

    /**
     * L2 归一化：向量除以模长。归一化后任意两向量的点积即余弦相似度。
     */
    private float[] normalize(float[] vec) {
        double sumSq = 0;
        for (float v : vec) {
            sumSq += v * v;
        }
        if (sumSq == 0) {
            return vec;
        }
        float norm = (float) Math.sqrt(sumSq);
        float[] out = new float[vec.length];
        for (int i = 0; i < vec.length; i++) {
            out[i] = vec[i] / norm;
        }
        return out;
    }

    @Override
    public String provider() {
        // @PostConstruct 一定先于任何调用执行；兜底防御 null
        return provider == null ? decideProvider() : provider;
    }

    @Override
    public int dimension() {
        return "siliconflow-bge-m3".equals(provider) ? 1024 : FALLBACK_DIM;
    }
}
