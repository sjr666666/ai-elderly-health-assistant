package com.example.backend.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 药品名称标准化处理器
 * 负责将识别到的药品名称进行标准化处理，确保与数据库存储名称一致
 */
@Component
public class DrugNameNormalizer {

    private static final Logger logger = LoggerFactory.getLogger(DrugNameNormalizer.class);

    /**
     * 常见药品名称变异规则库
     * key: 变异名称片段, value: 标准化后的名称
     */
    private static final Map<String, String> NAME_VARIATIONS = new HashMap<>();
    
    /**
     * 常见剂型词（用于识别药品名称）
     */
    private static final Set<String> DOSAGE_FORMS = new HashSet<>(Arrays.asList(
            "胶囊", "片剂", "分散片", "缓释片", "控释片",
            "肠溶片", "咀嚼片", "颗粒", "颗粒剂", "口服液", "注射液",
            "软膏", "乳膏", "凝胶", "喷雾剂", "气雾剂", "滴剂", "洗剂", "散剂",
            "丸", "滴丸", "胶囊剂", "软胶囊", "硬胶囊", "注射剂",
            "片", "剂", "胶囊", "口服液", "糖浆", "混悬液", "干混悬剂"
    ));

    /**
     * 常见前缀/后缀词库（需要移除的冗余词）
     */
    private static final Set<String> REDUNDANT_PREFIXES = new HashSet<>(Arrays.asList(
            "复方", "复方制剂", "口服", "外用", "医用", "药品", "药", "中药", "西药", "中成药"
    ));

    /**
     * 规格相关后缀（需要保留的）
     */
    private static final Set<String> SPEC_SUFFIXES = new HashSet<>(Arrays.asList(
            "mg", "g", "ml", "片", "粒", "袋", "瓶", "支", "盒"
    ));

    /**
     * 数字模式（用于提取规格信息）
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(mg|g|ml|片|粒|袋|瓶|支|盒)?");

    /**
     * 药品名称模式：匹配包含剂型的药品名称
     */
    private static final Pattern DRUG_NAME_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5a-zA-Z]+(胶囊|片剂|分散片|缓释片|控释片|肠溶片|咀嚼片|颗粒|口服液|注射液|软膏|乳膏|凝胶|喷雾剂|气雾剂|滴剂|洗剂|散剂|丸|滴丸|软胶囊|硬胶囊|片|剂|糖浆|混悬液|干混悬剂)");

    /**
     * 说明书内容关键词（需要过滤掉）
     */
    private static final Set<String> INSTRUCTION_KEYWORDS = new HashSet<>(Arrays.asList(
            "适应症", "适应", "用途", "功能主治", "主治", "用于",
            "用法", "用量", "用法用量", "服用", "口服",
            "注意", "注意事项", "禁忌", "不良反应", "副作用",
            "规格", "性状", "成分", "有效期", "生产日期",
            "生产批号", "批准文号", "国药准字",
            "贮藏", "包装", "有效期至", "执行标准"
    ));

    static {
        // 初始化药品名称变异规则库
        initNameVariations();
    }

    /**
     * 从OCR文本中提取核心药品名称
     * 过滤掉说明书上的其他内容（适应症、用法用量等）
     * 
     * @param rawText OCR识别的原始文本
     * @return 提取的核心药品名称，如果未能提取则返回null
     */
    public String extractDrugName(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return null;
        }

        logger.info("开始提取药品名称 - 原始文本长度: {} characters", rawText.length());

        // 将文本按行分割
        String[] lines = rawText.split("\\r?\\n");
        
        // 优先查找包含剂型的行
        String candidateName = null;
        int bestScore = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 跳过空行和太短的行
            if (trimmedLine.isEmpty() || trimmedLine.length() < 2) {
                continue;
            }

            // 检查是否是说明书内容（需要过滤）
            if (isInstructionContent(trimmedLine)) {
                logger.debug("跳过说明书内容行: {}", trimmedLine);
                continue;
            }

            // 检查是否包含药品名称模式
            Matcher matcher = DRUG_NAME_PATTERN.matcher(trimmedLine);
            if (matcher.find()) {
                String matchedName = matcher.group();
                
                // 评分：包含更多中文字符和剂型词的优先
                int score = calculateNameScore(matchedName);
                
                if (score > bestScore) {
                    bestScore = score;
                    candidateName = matchedName;
                    logger.debug("找到候选药品名称: {}, 评分: {}", matchedName, score);
                }
            }
            
            // 如果没有匹配到剂型模式，检查是否包含已知的药品关键词
            if (candidateName == null && containsDrugKeyword(trimmedLine)) {
                // 清理该行，移除规格等信息
                String cleanName = cleanLineForDrugName(trimmedLine);
                if (cleanName != null && cleanName.length() >= 2) {
                    candidateName = cleanName;
                    logger.debug("通过关键词匹配找到药品名称: {}", cleanName);
                }
            }
        }

        // 如果找到了候选名称，进行标准化处理
        if (candidateName != null) {
            String normalized = normalize(candidateName);
            logger.info("提取药品名称成功 - 提取: {}, 标准化后: {}", candidateName, normalized);
            return normalized;
        }

        logger.warn("未能从文本中提取药品名称");
        return null;
    }

    /**
     * 判断一行文本是否是说明书内容
     */
    private boolean isInstructionContent(String line) {
        String lowerLine = line.toLowerCase();
        for (String keyword : INSTRUCTION_KEYWORDS) {
            if (lowerLine.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查行是否包含已知的药品关键词
     */
    private boolean containsDrugKeyword(String line) {
        for (String keyword : NAME_VARIATIONS.keySet()) {
            if (line.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清理行文本，移除规格等信息，保留药品名称部分
     */
    private String cleanLineForDrugName(String line) {
        // 移除数字和单位（规格信息）
        String result = line.replaceAll("\\d+\\s*(mg|g|ml|片|粒|袋|瓶|支|盒|×|x|\\*)", "");
        // 移除特殊字符
        result = result.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z]", "");
        return result.trim();
    }

    /**
     * 计算药品名称候选的评分
     */
    private int calculateNameScore(String name) {
        int score = 0;
        
        // 中文字符越多评分越高
        for (char c : name.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                score += 2;
            }
        }
        
        // 包含剂型词加分
        for (String dosageForm : DOSAGE_FORMS) {
            if (name.contains(dosageForm)) {
                score += 10;
                break;
            }
        }
        
        // 包含已知药品关键词加分
        for (String keyword : NAME_VARIATIONS.keySet()) {
            if (name.contains(keyword)) {
                score += 5;
            }
        }
        
        return score;
    }

    private static void initNameVariations() {
        // 常见品牌名称变异
        NAME_VARIATIONS.put("999", "999");
        NAME_VARIATIONS.put("三九", "999");
        NAME_VARIATIONS.put("华润三九", "999");
        
        // 药品名称简写/别名
        NAME_VARIATIONS.put("感康", "复方氨酚烷胺片");
        NAME_VARIATIONS.put("白加黑", "氨酚伪麻美芬片");
        NAME_VARIATIONS.put("泰诺", "酚麻美敏片");
        NAME_VARIATIONS.put("康泰克", "复方盐酸伪麻黄碱缓释胶囊");
        NAME_VARIATIONS.put("芬必得", "布洛芬缓释胶囊");
        NAME_VARIATIONS.put("布洛芬", "布洛芬");
        
        // 感冒灵系列
        NAME_VARIATIONS.put("感冒灵", "感冒灵");
        NAME_VARIATIONS.put("999感冒灵", "999感冒灵");
        NAME_VARIATIONS.put("三九感冒灵", "999感冒灵");
        NAME_VARIATIONS.put("感冒灵颗粒", "感冒灵颗粒");
        NAME_VARIATIONS.put("999感冒灵颗粒", "999感冒灵颗粒");
        
        // 抗生素类
        NAME_VARIATIONS.put("阿莫西林", "阿莫西林");
        NAME_VARIATIONS.put("阿莫仙", "阿莫西林");
        NAME_VARIATIONS.put("安必仙", "阿莫西林");
        
        // 心血管类
        NAME_VARIATIONS.put("硝苯地平", "硝苯地平");
        NAME_VARIATIONS.put("心痛定", "硝苯地平");
        NAME_VARIATIONS.put("拜新同", "硝苯地平控释片");
        
        // 糖尿病类
        NAME_VARIATIONS.put("二甲双胍", "二甲双胍");
        NAME_VARIATIONS.put("格华止", "盐酸二甲双胍片");
        
        // 清热解毒类
        NAME_VARIATIONS.put("牛黄解毒", "牛黄解毒片");
        NAME_VARIATIONS.put("黄连上清", "黄连上清片");
        
        // 去除空格和特殊字符的映射
        NAME_VARIATIONS.put("复方甘草片", "复方甘草片");
        NAME_VARIATIONS.put("复方甘草", "复方甘草片");
    }

    /**
     * 标准化药品名称
     * 
     * @param rawName 原始识别名称
     * @return 标准化后的名称
     */
    public String normalize(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return null;
        }

        String normalized = rawName.trim();
        
        // 步骤1: 移除多余空格和特殊字符
        normalized = removeSpecialCharacters(normalized);
        logger.debug("步骤1-移除特殊字符: {}", normalized);
        
        // 步骤2: 应用名称变异规则
        normalized = applyVariationRules(normalized);
        logger.debug("步骤2-应用变异规则: {}", normalized);
        
        // 步骤3: 移除冗余前缀/后缀（保留规格相关）
        normalized = removeRedundantParts(normalized);
        logger.debug("步骤3-移除冗余部分: {}", normalized);
        
        // 步骤4: 统一大小写
        normalized = normalizeCase(normalized);
        logger.debug("步骤4-统一大小写: {}", normalized);
        
        // 步骤5: 移除多余空格
        normalized = normalized.replaceAll("\\s+", "");
        
        logger.info("药品名称标准化完成 - 原始: {}, 标准化后: {}", rawName, normalized);
        return normalized;
    }

    /**
     * 移除特殊字符和多余空格
     */
    private String removeSpecialCharacters(String name) {
        // 保留中文、英文、数字、空格、括号
        return name.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s\\(\\)（）]", "");
    }

    /**
     * 应用名称变异规则
     */
    private String applyVariationRules(String name) {
        String result = name;
        
        // 按规则库进行替换（按长度排序，优先匹配长的）
        List<String> sortedKeys = new ArrayList<>(NAME_VARIATIONS.keySet());
        sortedKeys.sort((a, b) -> b.length() - a.length());
        
        for (String key : sortedKeys) {
            if (result.contains(key)) {
                String replacement = NAME_VARIATIONS.get(key);
                // 如果替换后名称更准确，则进行替换
                if (replacement.length() >= key.length() || 
                    (replacement.length() < key.length() && !result.contains(replacement))) {
                    result = result.replace(key, replacement);
                    logger.debug("应用变异规则: {} -> {}", key, replacement);
                }
            }
        }
        
        return result;
    }

    /**
     * 移除冗余前缀/后缀
     */
    private String removeRedundantParts(String name) {
        String result = name;
        
        // 从后往前移除冗余后缀
        for (String suffix : REDUNDANT_PREFIXES) {
            if (result.endsWith(suffix)) {
                // 检查是否后面跟着规格信息，如果是则保留
                boolean keepSuffix = false;
                int suffixIndex = result.lastIndexOf(suffix);
                if (suffixIndex + suffix.length() < result.length()) {
                    String remaining = result.substring(suffixIndex + suffix.length());
                    Matcher matcher = NUMBER_PATTERN.matcher(remaining);
                    if (matcher.find()) {
                        keepSuffix = true;
                    }
                }
                
                if (!keepSuffix) {
                    result = result.substring(0, suffixIndex);
                    logger.debug("移除冗余后缀: {}", suffix);
                }
            }
        }
        
        // 从前往后移除冗余前缀
        for (String prefix : REDUNDANT_PREFIXES) {
            if (result.startsWith(prefix)) {
                result = result.substring(prefix.length());
                logger.debug("移除冗余前缀: {}", prefix);
            }
        }
        
        return result.trim();
    }

    /**
     * 统一大小写（药品名称通常使用中文或首字母大写的英文）
     */
    private String normalizeCase(String name) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : name.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext && Character.isLetter(c)) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    /**
     * 提取规格信息
     * 
     * @param text 原始文本
     * @return 规格字符串（如 "5mg*30片"）
     */
    public String extractSpecification(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        List<String> specParts = new ArrayList<>();
        
        // 查找数字+单位模式
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            specParts.add(matcher.group());
        }
        
        // 查找规格相关的特殊字符
        Pattern specPattern = Pattern.compile("(\\d+\\s*[×x*]\\s*\\d+\\s*(片|粒|袋|瓶|支|盒)?)");
        Matcher specMatcher = specPattern.matcher(text);
        if (specMatcher.find()) {
            specParts.add(specMatcher.group());
        }
        
        if (!specParts.isEmpty()) {
            String spec = String.join(" ", specParts);
            logger.debug("提取规格信息: {}", spec);
            return spec;
        }
        
        return null;
    }

    /**
     * 验证药品名称是否有效
     * 
     * @param name 药品名称
     * @return 是否有效
     */
    public boolean isValidDrugName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // 至少包含2个中文字符或4个英文字符
        String trimmed = name.trim();
        int chineseCount = 0;
        int letterCount = 0;
        
        for (char c : trimmed.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseCount++;
            } else if (Character.isLetter(c)) {
                letterCount++;
            }
        }
        
        return (chineseCount >= 2) || (letterCount >= 4);
    }

    /**
     * 计算两个药品名称的相似度
     * 
     * @param name1 名称1
     * @param name2 名称2
     * @return 相似度（0-1）
     */
    public double calculateSimilarity(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return 0.0;
        }
        
        String n1 = name1.toLowerCase().replaceAll("\\s", "");
        String n2 = name2.toLowerCase().replaceAll("\\s", "");
        
        if (n1.equals(n2)) {
            return 1.0;
        }
        
        // 使用Jaccard相似度
        Set<Character> set1 = new HashSet<>();
        Set<Character> set2 = new HashSet<>();
        
        for (char c : n1.toCharArray()) {
            set1.add(c);
        }
        for (char c : n2.toCharArray()) {
            set2.add(c);
        }
        
        // 计算交集
        Set<Character> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        // 计算并集
        Set<Character> union = new HashSet<>(set1);
        union.addAll(set2);
        
        if (union.isEmpty()) {
            return 0.0;
        }
        
        return (double) intersection.size() / union.size();
    }
}