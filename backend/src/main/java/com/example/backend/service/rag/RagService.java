package com.example.backend.service.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.util.SafetyGuard;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.UserMedicineBoxMapper;
import com.example.backend.model.dto.RagAnswer;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.UserMedicineBox;
import com.example.backend.service.DeepSeekService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 问答门面服务（检索增强生成）
 * <p>
 * 链路：<b>Retrieval（检索）→ Augmented（增强）→ Generation（生成）</b>
 * <ol>
 *   <li><b>检索</b>：问题向量化 → 向量索引 Top-K 召回语义相近的知识切片；
 *       向量检索无结果时降级为本地关键词倒排索引（KEYWORD 模式）</li>
 *   <li><b>增强</b>：把召回的知识切片按 [1][2] 编号拼进 prompt，作为生成的事实依据；
 *       同时注入<b>用户药箱上下文</b>（当前服用药品列表），实现基于真实用药的个性化回答</li>
 *   <li><b>生成</b>：DeepSeek 仅基于给定资料作答 + 引用标注 + 免责声明；
 *       LLM 不可用时降级为直接返回最相关切片原文（LOCAL 模式，保证离线可用）</li>
 * </ol>
 * 分级降级设计延续项目「AI + 本地双引擎」思想。
 */
@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    private final RagSearchService ragSearchService;
    private final DeepSeekService deepSeekService;
    private final UserMedicineBoxMapper userMedicineBoxMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final ObjectMapper objectMapper;

    @Value("${ai.rag.top-k:3}")
    private int topK;

    public RagService(RagSearchService ragSearchService,
                      DeepSeekService deepSeekService,
                      UserMedicineBoxMapper userMedicineBoxMapper,
                      DrugBaseMapper drugBaseMapper,
                      ObjectMapper objectMapper) {
        this.ragSearchService = ragSearchService;
        this.deepSeekService = deepSeekService;
        this.userMedicineBoxMapper = userMedicineBoxMapper;
        this.drugBaseMapper = drugBaseMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 回答问题：检索 → 增强 → 生成（不带用户上下文，兼容调用）
     */
    public RagAnswer ask(String question) {
        return ask(question, null);
    }

    /**
     * 回答问题：检索 → 增强 → 生成（带用户药箱上下文，回答个性化）
     *
     * @param question 用户问题
     * @param userId   当前登录用户ID（sys_user 主键），为 null 时不做个性化
     */
    public RagAnswer ask(String question, Long userId) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        long t0 = System.currentTimeMillis();

        // ========== 0. 用户上下文：当前药箱正在服用的药 ==========
        List<String> userDrugs = loadUserDrugNames(userId);

        // ========== 0.5 安全防护：prompt 注入 / 危险请求直接拒绝，不调用 LLM ==========
        if (!SafetyGuard.isSafe(question)) {
            return RagAnswer.builder()
                    .answer(SafetyGuard.refusalMessage())
                    .mode(RagAnswer.MODE_GUARDED)
                    .sources(new ArrayList<>())
                    .userDrugs(userDrugs)
                    .build();
        }

        // ========== 1. Retrieval 检索 ==========
        // 向量检索优先（语义匹配，能理解"这药和降压药一起吃行不行"这类自然问法），
        // 无结果时由 RagSearchService 自动降级为关键词倒排检索
        RagSearchService.SearchResult searchResult = ragSearchService.search(question, topK);
        String mode = searchResult.mode;
        List<VectorStore.ScoredChunk> scoredHits = searchResult.hits;
        if (scoredHits.isEmpty()) {
            logger.warn("[RAG] 检索无任何结果 - 问题: {}", question);
            return RagAnswer.builder()
                    .answer("暂时没有找到与这个问题相关的用药资料，建议咨询医生或药师。")
                    .mode(mode)
                    .sources(new ArrayList<>())
                    .build();
        }

        List<RagAnswer.Source> sources = toSources(scoredHits);

        // ========== 2. + 3. Augmented + Generation ==========
        String answer = generate(question, scoredHits, userDrugs);
        if (answer == null) {
            // LLM 不可用（未配 Key/调用失败），降级为本地知识直出，保证离线可用
            logger.warn("[RAG] LLM 生成失败，降级为本地知识直出");
            mode = RagAnswer.MODE_LOCAL;
            answer = SafetyGuard.appendDisclaimer(sources.get(0).getContent());
        }

        logger.info("[RAG] 问答完成 - 问题: {}, 模式: {}, 引用: {} 条, 用户药箱: {}, 耗时: {}ms",
                question, mode, sources.size(), userDrugs, System.currentTimeMillis() - t0);
        return RagAnswer.builder()
                .answer(answer)
                .mode(mode)
                .sources(sources)
                .userDrugs(userDrugs)
                .build();
    }

    /**
     * 加载用户药箱中「使用中」的药品名称列表（个性化上下文）
     */
    private List<String> loadUserDrugNames(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<UserMedicineBox> boxes = userMedicineBoxMapper.selectList(
                new LambdaQueryWrapper<UserMedicineBox>()
                        .eq(UserMedicineBox::getUserId, userId)
                        .eq(UserMedicineBox::getStatus, UserMedicineBox.Status.ACTIVE.getCode()));
        if (boxes == null || boxes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> drugIds = boxes.stream()
                .map(UserMedicineBox::getDrugId)
                .distinct()
                .collect(Collectors.toList());
        List<DrugBase> drugs = drugBaseMapper.selectBatchIds(drugIds);
        if (drugs == null) {
            return Collections.emptyList();
        }
        return drugs.stream()
                .map(d -> d.getGenericName() != null && !d.getGenericName().isEmpty()
                        ? d.getGenericName() : d.getTradeName())
                .filter(n -> n != null && !n.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 组装增强 prompt 并调用 LLM 生成回答（非流式）
     */
    private String generate(String question, List<VectorStore.ScoredChunk> hits, List<String> userDrugs) {
        return deepSeekService.chat(buildSystemPrompt(), buildUserPrompt(question, hits, userDrugs));
    }

    /**
     * RAG 流式问答：先发 meta（引用来源+药箱），再流式推送生成内容（打字机效果）
     * <p>
     * onEvent 回调 JSON 字符串事件：
     *   {"type":"meta","sources":[...],"userDrugs":[...]}
     *   {"type":"delta","content":"增量文本"}
     *   {"type":"done"}
     * LLM 不可用时（无 Key/失败）自动降级：meta 之后一次性推送本地知识直出，保证离线可用。
     */
    public void askStream(String question, Long userId, java.util.function.Consumer<String> onEvent) {
        // 0.5 安全防护：注入/危险请求直接拒绝，不检索不调用 LLM
        if (!SafetyGuard.isSafe(question)) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("type", "meta");
            meta.put("mode", RagAnswer.MODE_GUARDED);
            meta.put("userDrugs", new ArrayList<>());
            meta.put("sources", new ArrayList<>());
            try {
                onEvent.accept(objectMapper.writeValueAsString(meta));
            } catch (Exception e) {
                logger.error("[RAG] meta 序列化失败 - {}", e.getMessage());
            }
            sendDelta(onEvent, SafetyGuard.refusalMessage());
            onEvent.accept("{\"type\":\"done\"}");
            return;
        }
        // 0. 用户上下文 + 1. 检索（同步，快）
        List<String> userDrugs = loadUserDrugNames(userId);
        RagSearchService.SearchResult searchResult = ragSearchService.search(question, topK);
        List<RagAnswer.Source> sources = toSources(searchResult.hits);

        // 2. meta 事件：前端先展示来源与药箱，再等正文
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("type", "meta");
            meta.put("mode", searchResult.mode);
            meta.put("userDrugs", userDrugs);
            meta.put("sources", sources);
            onEvent.accept(objectMapper.writeValueAsString(meta));
        } catch (Exception e) {
            logger.error("[RAG] meta 序列化失败 - {}", e.getMessage());
        }

        if (sources.isEmpty()) {
            sendDelta(onEvent, "暂时没有找到与这个问题相关的用药资料，建议咨询医生或药师。");
            sendDelta(onEvent, "\n\n" + SafetyGuard.DISCLAIMER);
            onEvent.accept("{\"type\":\"done\"}");
            return;
        }

        // 3. 流式生成（逐块回调）
        StringBuilder received = new StringBuilder();
        deepSeekService.chatStream(buildSystemPrompt(),
                buildUserPrompt(question, searchResult.hits, userDrugs),
                delta -> {
                    received.append(delta);
                    sendDelta(onEvent, delta);
                });

        // LLM 不可用：流式没有任何内容 → 本地知识直出（保证离线可用）
        if (received.length() == 0) {
            logger.warn("[RAG] 流式生成无内容，降级为本地知识直出");
            String fallback = SafetyGuard.appendDisclaimer(sources.get(0).getContent());
            sendDelta(onEvent, fallback);
        }
        onEvent.accept("{\"type\":\"done\"}");
    }

    private void sendDelta(java.util.function.Consumer<String> onEvent, String content) {
        try {
            Map<String, Object> delta = new HashMap<>();
            delta.put("type", "delta");
            delta.put("content", content);
            onEvent.accept(objectMapper.writeValueAsString(delta));
        } catch (Exception e) {
            logger.error("[RAG] delta 序列化失败 - {}", e.getMessage());
        }
    }

    /**
     * 构建系统提示（角色 + 引用 + 免责约束）
     */
    private String buildSystemPrompt() {
        StringBuilder system = new StringBuilder();
        system.append("你是「AI药管家」的用药知识助手，服务对象是老年人及其家属。请遵守以下规则：\n");
        system.append("1. 只根据「参考资料」中的内容回答，参考资料中没有的信息，明确说'资料中没有提到'，不要编造；\n");
        system.append("2. 用通俗易懂的大白话回答，多打比方，避免专业术语；\n");
        system.append("3. 引用参考资料时用[1][2]编号标注来源；\n");
        system.append("4. 参考资料中与问题无关的条目（如提及完全不同的药品/疾病）不要引用，直接忽略，宁可少引；\n");
        system.append("5. 回答最后必须加上一句：以上信息仅供参考，具体用药请遵医嘱；\n");
        system.append("6. 不做诊断、不推荐替代治疗方案，遇到紧急情况提示立即就医。");
        return system.toString();
    }

    /**
     * 构建用户提示（参考资料 + 药箱上下文 + 问题）
     */
    private String buildUserPrompt(String question, List<VectorStore.ScoredChunk> hits, List<String> userDrugs) {
        StringBuilder user = new StringBuilder();
        user.append("参考资料：\n");
        for (int i = 0; i < hits.size(); i++) {
            user.append("[").append(i + 1).append("] ")
                    .append(hits.get(i).chunk.getTitle());
            if (hits.get(i).chunk.getSourceRef() != null && !hits.get(i).chunk.getSourceRef().isEmpty()) {
                user.append("（来源：").append(hits.get(i).chunk.getSourceRef()).append("）");
            }
            user.append("\n").append(hits.get(i).chunk.getContent()).append("\n\n");
        }
        if (userDrugs != null && !userDrugs.isEmpty()) {
            user.append("用户当前药箱正在服用的药：").append(String.join("、", userDrugs))
                    .append("。如果问题与这些药有关，请优先结合它们回答，让老人感觉你了解他的用药情况。\n\n");
        }
        user.append("老人的问题：").append(question);
        return user.toString();
    }

    private List<RagAnswer.Source> toSources(List<VectorStore.ScoredChunk> hits) {
        List<RagAnswer.Source> sources = new ArrayList<>(hits.size());
        for (VectorStore.ScoredChunk hit : hits) {
            sources.add(RagAnswer.Source.builder()
                    .title(hit.chunk.getTitle())
                    .sourceType(hit.chunk.getSourceType())
                    .content(truncate(hit.chunk.getContent(), 200))
                    .sourceRef(hit.chunk.getSourceRef())
                    .score(hit.score)
                    .build());
        }
        return sources;
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }
}
