package com.example.backend.service.rag;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG 知识库入库服务单测
 * 验证：Markdown front-matter 解析正确性 + 知识资源文件完整性（加知识=加文件 的约定）
 */
class RagIngestServiceTest {

    private final RagIngestService service =
            new RagIngestService(null, null, null, null, null, null);

    @Test
    void frontMatter解析出标题来源与正文() {
        String md = "---\n" +
                "title: 高血压\n" +
                "source: 参考整理（仅供科普，具体遵医嘱）\n" +
                "tags: [高血压, 慢病]\n" +
                "---\n" +
                "高血压是常见慢性病，需要规律服药。";
        RagIngestService.MarkdownDoc doc = service.parseMarkdown(md);
        assertNotNull(doc);
        assertEquals("高血压", doc.title);
        assertEquals("参考整理（仅供科普，具体遵医嘱）", doc.source);
        assertEquals("高血压 慢病", doc.tags);
        assertEquals("高血压是常见慢性病，需要规律服药。", doc.body);
    }

    @Test
    void 无frontMatter时回退到标题行() {
        String md = "# 高血压\n\n高血压是常见慢性病。";
        RagIngestService.MarkdownDoc doc = service.parseMarkdown(md);
        assertNotNull(doc);
        assertEquals("高血压", doc.title);
        assertEquals("高血压是常见慢性病。", doc.body);
    }

    @Test
    void 无标题文件返回null() {
        assertNull(service.parseMarkdown("正文没有标题"));
    }

    @Test
    void 指南与FAQ资源文件数量符合约定() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] guides = resolver.getResources("classpath:knowledge/guides/*.md");
        Resource[] faqs = resolver.getResources("classpath:knowledge/faqs/*.md");
        assertEquals(10, guides.length, "慢病指南应为 10 个文件");
        assertEquals(12, faqs.length, "用药 FAQ 应为 12 个文件");
    }

    @Test
    void 开源药品知识文件已采集且数量可观() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] drugs = resolver.getResources("classpath:knowledge/drugs/*.md");
        assertTrue(drugs.length >= 500, "开源数据集药品知识应 >= 500 条，实际 " + drugs.length);
    }

    @Test
    void 每个知识文件frontMatter完整可解析() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<Resource> all = new ArrayList<>();
        all.addAll(List.of(resolver.getResources("classpath:knowledge/guides/*.md")));
        all.addAll(List.of(resolver.getResources("classpath:knowledge/faqs/*.md")));
        all.addAll(List.of(resolver.getResources("classpath:knowledge/drugs/*.md")));

        for (Resource r : all) {
            String raw = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            RagIngestService.MarkdownDoc doc = service.parseMarkdown(raw);
            assertNotNull(doc, "文件解析不应为 null: " + r.getFilename());
            assertFalse(doc.title.isEmpty(), "title 不能为空: " + r.getFilename());
            assertFalse(doc.source.isEmpty(), "source 不能为空: " + r.getFilename());
            assertFalse(doc.body.isEmpty(), "正文不能为空: " + r.getFilename());
        }
    }
}
