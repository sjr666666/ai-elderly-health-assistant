package com.example.backend.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 药品名称提取工具单元测试
 * 覆盖:说明书文本提取、说明书内容过滤、空输入防护
 */
class DrugNameNormalizerTest {

    private final DrugNameNormalizer normalizer = new DrugNameNormalizer();

    @Test
    void extractDrugName_fromFullInstructionText_returnsDrugName() {
        String ocrText = "感冒灵颗粒\n"
                + "【药品名称】感冒灵颗粒\n"
                + "【成　份】三叉苦、金盏银盘、野菊花\n"
                + "【用法用量】开水冲服，一次1袋，一日3次\n"
                + "【生产企业】华润三九医药股份有限公司";
        String result = normalizer.extractDrugName(ocrText);
        assertNotNull(result);
        assertTrue(result.contains("感冒灵"), "应提取出药品名,实际: " + result);
    }

    @Test
    void extractDrugName_nullOrBlank_returnsNull() {
        assertNull(normalizer.extractDrugName(null));
        assertNull(normalizer.extractDrugName(""));
        assertNull(normalizer.extractDrugName("   "));
    }

    @Test
    void extractDrugName_onlyInstructionContent_returnsNull() {
        // 只有说明书内容,没有药品名模式
        String text = "【适应症】解热镇痛\n【用法用量】口服\n【贮藏】密封保存";
        assertNull(normalizer.extractDrugName(text));
    }

    @Test
    void extractDrugName_withSpecification_cleansSpec() {
        String ocrText = "阿莫西林胶囊 0.5g*24粒\n国药准字H20003263";
        String result = normalizer.extractDrugName(ocrText);
        assertNotNull(result);
        assertTrue(result.contains("阿莫西林"), "应提取出药品名,实际: " + result);
    }

    @Test
    void extractDrugName_shortNoiseLines_returnsNull() {
        assertNull(normalizer.extractDrugName("a\nb\n1\n2"));
    }
}
