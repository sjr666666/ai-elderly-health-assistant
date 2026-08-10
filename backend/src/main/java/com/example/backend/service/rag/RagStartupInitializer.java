package com.example.backend.service.rag;

import com.example.backend.mapper.KnowledgeChunkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时自动构建 RAG 知识库
 * <p>
 * 若 knowledge_chunk 表为空且配置开启（默认开启），则自动执行全量入库并重建索引。
 * 之后每次重启都从库中恢复内存索引，无需人工干预。
 */
@Component
public class RagStartupInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RagStartupInitializer.class);

    private final RagIngestService ragIngestService;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final VectorStore vectorStore;
    private final KeywordIndex keywordIndex;

    @Value("${ai.rag.ingest-on-startup:true}")
    private boolean ingestOnStartup;

    public RagStartupInitializer(RagIngestService ragIngestService,
                                 KnowledgeChunkMapper knowledgeChunkMapper,
                                 VectorStore vectorStore,
                                 KeywordIndex keywordIndex) {
        this.ragIngestService = ragIngestService;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.vectorStore = vectorStore;
        this.keywordIndex = keywordIndex;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Long count = knowledgeChunkMapper.selectCount(null);
            if (count == null || count == 0) {
                if (ingestOnStartup) {
                    logger.info("[RAG] 知识库为空，启动时自动入库...");
                    ragIngestService.ingestAll();
                }
            } else {
                // 已有数据，直接从库恢复内存索引
                logger.info("[RAG] 知识库已有 {} 条切片，从库中恢复内存索引", count);
                java.util.List<com.example.backend.model.entity.KnowledgeChunk> all =
                        knowledgeChunkMapper.selectList(null);
                vectorStore.rebuild(all);
                keywordIndex.rebuild(all);
            }
        } catch (Exception e) {
            // 入库失败不阻塞启动（如 embedding 服务不可用），仅记录日志
            logger.error("[RAG] 启动初始化知识库失败 - {}", e.getMessage());
        }
    }
}
