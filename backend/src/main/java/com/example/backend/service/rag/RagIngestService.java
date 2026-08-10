package com.example.backend.service.rag;

import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.KnowledgeChunkMapper;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.KnowledgeChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 知识库入库服务
 * <p>
 * 三路知识源（内容与代码分离，加知识 = 加数据/文件，不用改代码）：
 * <ol>
 *   <li><b>DRUG</b>：动态，从 drug_base 药品基础库抽取（通用名/商品名/规格/分类/说明书原文）</li>
 *   <li><b>GUIDE</b>：资源文件，扫描 classpath:knowledge/guides/*.md（慢病管理指南）</li>
 *   <li><b>FAQ</b>：资源文件，扫描 classpath:knowledge/faqs/*.md（老人高频用药问答）</li>
 * </ol>
 * Markdown 文件头部为 YAML front-matter（title/source/tags），正文为知识内容，
 * 每条文件 = 一个知识切片。入库流程：生成切片 → 向量化 → 落库 → 重建内存索引。
 */
@Service
public class RagIngestService {

    private static final Logger logger = LoggerFactory.getLogger(RagIngestService.class);

    private static final PathMatchingResourcePatternResolver RESOLVER =
            new PathMatchingResourcePatternResolver();

    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final KeywordIndex keywordIndex;
    private final ObjectMapper objectMapper;
    private final Yaml yaml = new Yaml();

    public RagIngestService(KnowledgeChunkMapper knowledgeChunkMapper,
                            DrugBaseMapper drugBaseMapper,
                            EmbeddingService embeddingService,
                            VectorStore vectorStore,
                            KeywordIndex keywordIndex,
                            ObjectMapper objectMapper) {
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.drugBaseMapper = drugBaseMapper;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.keywordIndex = keywordIndex;
        this.objectMapper = objectMapper;
    }

    /**
     * 全量入库并重建索引（幂等：先清空再灌入）
     *
     * @return 入库的知识切片总数
     */
    public synchronized int ingestAll() {
        long t0 = System.currentTimeMillis();
        List<KnowledgeChunk> chunks = new ArrayList<>();
        chunks.addAll(buildDrugChunks());
        chunks.addAll(loadMarkdownKnowledge("knowledge/guides", KnowledgeChunk.SOURCE_TYPE_GUIDE));
        chunks.addAll(loadMarkdownKnowledge("knowledge/faqs", KnowledgeChunk.SOURCE_TYPE_FAQ));

        // 向量化（title 权重高于 content，故拼接 title 在前）
        for (KnowledgeChunk chunk : chunks) {
            String text = chunk.getTitle() + " " + chunk.getContent();
            float[] vec = embeddingService.embed(text);
            try {
                chunk.setEmbeddingJson(objectMapper.writeValueAsString(vec));
            } catch (Exception e) {
                logger.error("[RAG] 切片 {} 向量序列化失败 - {}", chunk.getTitle(), e.getMessage());
            }
        }

        // 幂等重灌
        knowledgeChunkMapper.delete(null);
        for (KnowledgeChunk chunk : chunks) {
            knowledgeChunkMapper.insert(chunk);
        }

        // 重建内存索引
        List<KnowledgeChunk> all = knowledgeChunkMapper.selectList(null);
        vectorStore.rebuild(all);
        keywordIndex.rebuild(all);

        long guideCount = chunks.stream()
                .filter(c -> KnowledgeChunk.SOURCE_TYPE_GUIDE.equals(c.getSourceType())).count();
        long faqCount = chunks.stream()
                .filter(c -> KnowledgeChunk.SOURCE_TYPE_FAQ.equals(c.getSourceType())).count();
        long drugCount = chunks.size() - guideCount - faqCount;

        logger.info("[RAG] 知识库入库完成：{} 条切片（药品 {} / 指南 {} / FAQ {}），耗时 {}ms，provider={}",
                all.size(), drugCount, guideCount, faqCount,
                System.currentTimeMillis() - t0, embeddingService.provider());
        return all.size();
    }

    /**
     * 从药品基础库生成知识切片（动态，业务表新增药品自动进入知识库）
     */
    private List<KnowledgeChunk> buildDrugChunks() {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        List<DrugBase> drugs = drugBaseMapper.selectList(null);
        if (drugs == null) {
            return chunks;
        }
        for (DrugBase drug : drugs) {
            String name = drug.getGenericName();
            if (name == null || name.isEmpty()) {
                name = drug.getTradeName();
            }
            if (name == null || name.isEmpty()) {
                continue;
            }
            StringBuilder content = new StringBuilder();
            if (drug.getTradeName() != null && !drug.getTradeName().isEmpty()) {
                content.append("商品名：").append(drug.getTradeName()).append("；");
            }
            if (drug.getSpecification() != null && !drug.getSpecification().isEmpty()) {
                content.append("规格：").append(drug.getSpecification()).append("；");
            }
            if (drug.getCategory() != null && !drug.getCategory().isEmpty()) {
                content.append("分类：").append(drug.getCategory()).append("；");
            }
            if (drug.getManufacturer() != null && !drug.getManufacturer().isEmpty()) {
                content.append("厂家：").append(drug.getManufacturer()).append("；");
            }
            if (drug.getDescription() != null && !drug.getDescription().isEmpty()) {
                content.append("说明：").append(drug.getDescription());
            }
            chunks.add(KnowledgeChunk.builder()
                    .sourceType(KnowledgeChunk.SOURCE_TYPE_DRUG)
                    .sourceId(drug.getId())
                    .title(name)
                    .content(content.toString())
                    .keywords(join(drug.getGenericName(), drug.getTradeName(), drug.getCommonName()))
                    .sourceRef("药品基础库（演示数据，具体以官方说明书为准）")
                    .build());
        }
        return chunks;
    }

    /**
     * 扫描 classpath 下的 Markdown 知识文件（一个文件 = 一条知识切片）
     * <p>
     * 文件格式：
     * <pre>
     * ---
     * title: 高血压
     * source: 参考整理（仅供科普，具体遵医嘱）
     * tags: [高血压, 慢病]
     * ---
     * 正文内容...
     * </pre>
     * 加知识 = 在对应目录加一个 .md 文件，重启后端即自动入库，无需改代码。
     */
    private List<KnowledgeChunk> loadMarkdownKnowledge(String dir, String sourceType) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        try {
            Resource[] resources = RESOLVER.getResources("classpath:" + dir + "/*.md");
            for (Resource resource : resources) {
                try {
                    String raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    MarkdownDoc doc = parseMarkdown(raw);
                    if (doc == null || doc.title == null || doc.title.isEmpty()) {
                        logger.warn("[RAG] 跳过无标题的知识文件: {}", resource.getFilename());
                        continue;
                    }
                    chunks.add(KnowledgeChunk.builder()
                            .sourceType(sourceType)
                            .title(doc.title)
                            .content(doc.body)
                            .keywords(doc.title + " " + doc.tags)
                            .sourceRef(doc.source)
                            .build());
                } catch (Exception e) {
                    logger.warn("[RAG] 解析知识文件失败 {} - {}", resource.getFilename(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("[RAG] 扫描知识目录失败 {} - {}", dir, e.getMessage());
        }
        return chunks;
    }

    /**
     * 解析 Markdown 文件的 front-matter（--- 包裹的 YAML 头）与正文
     */
    private MarkdownDoc parseMarkdown(String raw) {
        String title = null;
        String source = null;
        String tags = "";
        String body = raw.trim();

        // 提取 --- 之间的 YAML front-matter
        if (body.startsWith("---")) {
            int end = body.indexOf("\n---", 3);
            if (end > 0) {
                String frontMatter = body.substring(3, end);
                String rest = body.substring(end + 4).trim();
                try {
                    Map<String, Object> meta = yaml.load(frontMatter);
                    if (meta != null) {
                        Object t = meta.get("title");
                        if (t != null) {
                            title = t.toString();
                        }
                        Object s = meta.get("source");
                        if (s != null) {
                            source = s.toString();
                        }
                        Object tg = meta.get("tags");
                        if (tg instanceof List) {
                            tags = String.join(" ", (List<String>) tg);
                        } else if (tg != null) {
                            tags = tg.toString();
                        }
                    }
                } catch (Exception e) {
                    logger.warn("[RAG] front-matter 解析失败，仅提取标题行 - {}", e.getMessage());
                    title = fallbackTitle(body);
                }
                body = rest;
            }
        }
        // front-matter 缺失时尝试从第一个 # 标题提取
        if (title == null) {
            title = fallbackTitle(body);
        }
        if (title == null) {
            return null;
        }
        return new MarkdownDoc(title, source == null ? "参考整理（仅供科普，具体遵医嘱）" : source, tags, body);
    }

    private String fallbackTitle(String text) {
        for (String line : text.split("\n")) {
            String l = line.trim();
            if (l.startsWith("# ")) {
                return l.substring(2).trim();
            }
        }
        return null;
    }

    private static final class MarkdownDoc {
        final String title;
        final String source;
        final String tags;
        final String body;

        MarkdownDoc(String title, String source, String tags, String body) {
            this.title = title;
            this.source = source;
            this.tags = tags;
            this.body = body;
        }
    }

    private String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isEmpty()) {
                sb.append(p).append(' ');
            }
        }
        return sb.toString().trim();
    }
}
