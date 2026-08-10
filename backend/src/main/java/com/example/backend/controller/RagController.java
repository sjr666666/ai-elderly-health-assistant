package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.RagAnswer;
import com.example.backend.service.rag.EmbeddingService;
import com.example.backend.service.rag.KeywordIndex;
import com.example.backend.service.rag.RagIngestService;
import com.example.backend.service.rag.RagService;
import com.example.backend.service.rag.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RAG 用药知识库问答控制器
 * 老人/家属端入口：用药知识问答（检索增强生成，带引用来源 + 用户药箱个性化）
 */
@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RagController {

    private static final Logger logger = LoggerFactory.getLogger(RagController.class);

    private final RagService ragService;
    private final RagIngestService ragIngestService;
    private final VectorStore vectorStore;
    private final KeywordIndex keywordIndex;
    private final EmbeddingService embeddingService;

    public RagController(RagService ragService,
                         RagIngestService ragIngestService,
                         VectorStore vectorStore,
                         KeywordIndex keywordIndex,
                         EmbeddingService embeddingService) {
        this.ragService = ragService;
        this.ragIngestService = ragIngestService;
        this.vectorStore = vectorStore;
        this.keywordIndex = keywordIndex;
        this.embeddingService = embeddingService;
    }

    /**
     * 用药知识问答（带用户药箱个性化）
     * 请求体: {"question": "阿司匹林和降压药能一起吃吗？"}
     * 返回:   {answer, mode, sources[], userDrugs[]}
     */
    @PostMapping("/ask")
    public ResponseResult<RagAnswer> ask(@RequestBody Map<String, String> body) {
        String question = body == null ? null : body.get("question");
        if (question == null || question.trim().isEmpty()) {
            return ResponseResult.fail("问题不能为空");
        }
        logger.info("收到 RAG 问答请求 - 问题: {}", question);
        // 从 JWT 上下文取当前用户ID，用于注入药箱上下文（个性化回答）
        RagAnswer answer = ragService.ask(question.trim(), getCurrentUserId());
        return ResponseResult.success("问答完成", answer);
    }

    /**
     * 获取当前认证用户的ID（数据库主键）；未认证时返回 null（不阻断问答）
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 触发知识库全量入库（幂等：先清空再灌入），通常只在启动/种子数据变更时调用
     */
    @PostMapping("/ingest")
    public ResponseResult<Map<String, Object>> ingest() {
        int count = ragIngestService.ingestAll();
        Map<String, Object> data = new HashMap<>();
        data.put("chunkCount", count);
        data.put("provider", embeddingService.provider());
        return ResponseResult.success("知识库入库完成", data);
    }

    /**
     * 知识库健康状态（排查/演示用）
     */
    @GetMapping("/health")
    public ResponseResult<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("vectorIndexSize", vectorStore.size());
        data.put("keywordIndexSize", keywordIndex.size());
        data.put("embeddingProvider", embeddingService.provider());
        data.put("embeddingDim", embeddingService.dimension());
        return ResponseResult.success("OK", data);
    }
}
