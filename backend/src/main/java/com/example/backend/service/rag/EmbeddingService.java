package com.example.backend.service.rag;

/**
 * Embedding 向量化服务
 * 将文本转为稠密向量，用于 RAG 检索阶段的相似度计算。
 * 设计为接口 + 双实现，便于讲解与替换：
 *   - 真实方案：SiliconFlow bge-m3（DeepSeek 无向量接口，故走 OpenAI 兼容的第三方）
 *   - 降级方案：无 API Key 时使用内置哈希特征向量，保证链路可跑通
 */
public interface EmbeddingService {

    /**
     * 将文本转为向量
     *
     * @param text 输入文本
     * @return 归一化后的浮点向量（L2 归一化，可直接做点积比较余弦相似度）
     */
    float[] embed(String text);

    /**
     * 当前使用的向量化提供商名称，用于日志与展示
     * 如 "siliconflow-bge-m3" / "local-hash-fallback"
     */
    String provider();

    /**
     * 向量维度
     */
    int dimension();
}
