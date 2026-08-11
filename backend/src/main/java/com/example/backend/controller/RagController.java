package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.common.util.SnowflakeIdGenerator;
import com.example.backend.mapper.RagFeedbackMapper;
import com.example.backend.model.dto.RagAnswer;
import com.example.backend.model.entity.RagFeedback;
import com.example.backend.service.rag.EmbeddingService;
import com.example.backend.service.rag.KeywordIndex;
import com.example.backend.service.rag.RagIngestService;
import com.example.backend.service.rag.RagService;
import com.example.backend.service.rag.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
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
    private final RagFeedbackMapper ragFeedbackMapper;
    private final SnowflakeIdGenerator idGenerator;

    public RagController(RagService ragService,
                         RagIngestService ragIngestService,
                         VectorStore vectorStore,
                         KeywordIndex keywordIndex,
                         EmbeddingService embeddingService,
                         RagFeedbackMapper ragFeedbackMapper,
                         SnowflakeIdGenerator idGenerator) {
        this.ragService = ragService;
        this.ragIngestService = ragIngestService;
        this.vectorStore = vectorStore;
        this.keywordIndex = keywordIndex;
        this.embeddingService = embeddingService;
        this.ragFeedbackMapper = ragFeedbackMapper;
        this.idGenerator = idGenerator;
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
     * 用药知识问答（流式 SSE，打字机效果，老人等待时有实时反馈）
     * 请求体: {"question": "..."}
     * 事件流: data: {"type":"meta","sources":[...],"userDrugs":[...]}
     *         data: {"type":"delta","content":"增量文本"}
     *         data: {"type":"done"}
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestBody Map<String, String> body) {
        String question = body == null ? null : body.get("question");
        SseEmitter emitter = new SseEmitter(180_000L);
        if (question == null || question.trim().isEmpty()) {
            emitter.completeWithError(new IllegalArgumentException("问题不能为空"));
            return emitter;
        }
        final String finalQuestion = question.trim();
        final Long userId = getCurrentUserId();
        executor.execute(() -> {
            try {
                ragService.askStream(finalQuestion, userId, eventJson -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(eventJson, MediaType.APPLICATION_JSON));
                    } catch (Exception sendEx) {
                        logger.warn("[RAG] SSE 事件发送失败 - {}", sendEx.getMessage());
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("{}", MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                logger.error("[RAG] 流式问答异常 - {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /** 流式问答专用线程池（避免阻塞 HTTP 线程） */
    private final java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newFixedThreadPool(2);

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
     * 增量入库单个药品（幂等）：新药写入 drug_base 后调用，无需全量重灌
     */
    @PostMapping("/ingest/drug/{drugId}")
    public ResponseResult<Map<String, Object>> ingestDrug(@PathVariable Long drugId) {
        int count = ragIngestService.ingestDrug(drugId);
        Map<String, Object> data = new HashMap<>();
        data.put("chunkCount", count);
        data.put("provider", embeddingService.provider());
        return ResponseResult.success(count > 0 ? "药品增量入库完成" : "药品不存在或无需入库", data);
    }

    /**
     * 回答质量反馈：前端「这个回答有用吗」👍/👎 落库（评测集校准 + 质量监控）
     * 请求体: {"question":"...","answer":"...","rating":1,"mode":"VECTOR"}
     * rating: 1=有用，-1=没用
     */
    @PostMapping("/feedback")
    public ResponseResult<Map<String, Object>> feedback(@RequestBody RagFeedbackRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return ResponseResult.fail("问题不能为空");
        }
        if (request.getRating() == null || (request.getRating() != 1 && request.getRating() != -1)) {
            return ResponseResult.fail("评分只能是 1（有用）或 -1（没用）");
        }
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseResult.fail("请先登录后再反馈");
        }

        RagFeedback feedback = new RagFeedback();
        feedback.setId(idGenerator.nextId());
        feedback.setUserId(userId);
        feedback.setQuestion(request.getQuestion().trim());
        feedback.setAnswer(request.getAnswer());
        feedback.setRating(request.getRating());
        feedback.setMode(request.getMode());
        feedback.setCreatedAt(LocalDateTime.now());
        ragFeedbackMapper.insert(feedback);

        logger.info("收到 RAG 回答反馈 - userId: {}, rating: {}, question: {}", userId, request.getRating(), request.getQuestion());
        Map<String, Object> data = new HashMap<>();
        data.put("feedbackId", feedback.getId());
        return ResponseResult.success("反馈已记录，谢谢您的帮助", data);
    }

    /** 反馈请求体 */
    public static class RagFeedbackRequest {
        private String question;
        private String answer;
        private Integer rating;
        private String mode;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
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
