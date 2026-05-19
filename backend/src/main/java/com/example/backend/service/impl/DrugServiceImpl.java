package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.model.dto.DrugInfoResponse;
import com.example.backend.model.dto.DrugSearchResponse;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.service.DeepSeekService;
import com.example.backend.service.DrugService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 药品服务实现类
 */
@Service
public class DrugServiceImpl implements DrugService {

    private static final Logger logger = LoggerFactory.getLogger(DrugServiceImpl.class);

    private final DrugBaseMapper drugBaseMapper;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper;

    @Autowired
    public DrugServiceImpl(DrugBaseMapper drugBaseMapper, DeepSeekService deepSeekService, ObjectMapper objectMapper) {
        this.drugBaseMapper = drugBaseMapper;
        this.deepSeekService = deepSeekService;
        this.objectMapper = objectMapper;
    }

    // 药品类别关键词映射
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new HashMap<>();
    static {
        CATEGORY_KEYWORDS.put("感冒药", Arrays.asList("感冒", "流感", "发烧", "咳嗽", "鼻塞", "流涕", "咽痛"));
        CATEGORY_KEYWORDS.put("止痛药", Arrays.asList("疼痛", "头痛", "牙痛", "关节痛", "腰痛", "止痛"));
        CATEGORY_KEYWORDS.put("退烧药", Arrays.asList("退烧", "发热", "体温", "发烧"));
        CATEGORY_KEYWORDS.put("消炎药", Arrays.asList("消炎", "抗炎", "红肿", "发炎"));
        CATEGORY_KEYWORDS.put("胃药", Arrays.asList("胃", "胃痛", "胃酸", "胃胀", "消化"));
        CATEGORY_KEYWORDS.put("降压药", Arrays.asList("血压", "降压", "高血压"));
        CATEGORY_KEYWORDS.put("降糖药", Arrays.asList("血糖", "降糖", "糖尿病"));
        CATEGORY_KEYWORDS.put("心脏病药", Arrays.asList("心脏", "心律", "心肌"));
        CATEGORY_KEYWORDS.put("抗过敏药", Arrays.asList("过敏", "皮肤痒", "荨麻疹"));
        CATEGORY_KEYWORDS.put("镇静催眠药", Arrays.asList("失眠", "睡眠", "安眠"));
    }

    // 常见药品别名映射
    private static final Map<String, String> DRUG_ALIASES = new HashMap<>();
    static {
        DRUG_ALIASES.put("扑热息痛", "对乙酰氨基酚");
        DRUG_ALIASES.put("泰诺", "对乙酰氨基酚");
        DRUG_ALIASES.put("泰诺林", "对乙酰氨基酚");
        DRUG_ALIASES.put("百服宁", "对乙酰氨基酚");
        DRUG_ALIASES.put("芬必得", "布洛芬");
        DRUG_ALIASES.put("美林", "布洛芬");
        DRUG_ALIASES.put("安瑞克", "布洛芬");
        DRUG_ALIASES.put("乙酰水杨酸", "阿司匹林");
        DRUG_ALIASES.put("心痛定", "硝苯地平");
        DRUG_ALIASES.put("拜新同", "硝苯地平");
        DRUG_ALIASES.put("格华止", "二甲双胍");
        DRUG_ALIASES.put("阿莫仙", "阿莫西林");
        DRUG_ALIASES.put("安必仙", "阿莫西林");
        DRUG_ALIASES.put("世福素", "头孢克肟");
        DRUG_ALIASES.put("达力芬", "头孢克肟");
        DRUG_ALIASES.put("思密达", "蒙脱石散");
        DRUG_ALIASES.put("洛赛克", "奥美拉唑");
        DRUG_ALIASES.put("奥克", "奥美拉唑");
        DRUG_ALIASES.put("开瑞坦", "氯雷他定");
        DRUG_ALIASES.put("息斯敏", "氯雷他定");
    }

    @Override
    public List<DrugInfoResponse> getDrugList(String keyword) {
        logger.info("查询药品列表 - keyword: {}", keyword);

        LambdaQueryWrapper<DrugBase> queryWrapper = new LambdaQueryWrapper<>();
        
        // 如果有关键词，进行模糊搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(DrugBase::getGenericName, keyword)
                    .or()
                    .like(DrugBase::getTradeName, keyword)
                    .or()
                    .like(DrugBase::getCommonName, keyword)
                    .or()
                    .like(DrugBase::getManufacturer, keyword)
            );
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc(DrugBase::getCreatedAt);

        List<DrugBase> drugList = drugBaseMapper.selectList(queryWrapper);

        // 转换为响应 DTO
        List<DrugInfoResponse> responseList = drugList.stream()
                .map(drug -> {
                    String displayText = String.format("%s (%s) - %s",
                            drug.getGenericName(),
                            drug.getSpecification(),
                            drug.getManufacturer());
                    
                    return DrugInfoResponse.builder()
                            .id(drug.getId())
                            .drugName(drug.getGenericName())
                            .specification(drug.getSpecification())
                            .manufacturer(drug.getManufacturer())
                            .displayText(displayText)
                            .build();
                })
                .collect(Collectors.toList());

        logger.info("查询到药品数量: {}", responseList.size());
        return responseList;
    }

    @Override
    public DrugDetailResponse getDrugDetailByName(String drugName) {
        logger.info("查询药品详细信息 - drugName: {}", drugName);

        // 首先尝试调用DeepSeek AI查询药品信息
        DrugDetailResponse aiResponse = deepSeekService.queryDrugInfoWithAI(drugName);
        if (aiResponse != null) {
            logger.info("成功从DeepSeek AI获取药品信息");
            // 如果AI返回了通用名，则使用AI的数据
            return aiResponse;
        }

        logger.info("DeepSeek AI查询失败或未配置，回退到数据库查询");
        
        // AI查询失败或未配置，回退到数据库查询
        LambdaQueryWrapper<DrugBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .like(DrugBase::getGenericName, drugName)
                .or()
                .like(DrugBase::getTradeName, drugName)
                .or()
                .like(DrugBase::getCommonName, drugName)
        );
        // 按通用名精确匹配优先排序
        queryWrapper.orderByDesc(DrugBase::getGenericName);

        // 使用 selectList 避免多条记录时抛异常
        List<DrugBase> drugList = drugBaseMapper.selectList(queryWrapper);

        if (drugList == null || drugList.isEmpty()) {
            logger.warn("未找到药品: {}", drugName);
            return null;
        }

        // 优先选择通用名完全匹配的记录
        DrugBase drug = drugList.stream()
                .filter(d -> d.getGenericName() != null && d.getGenericName().equals(drugName))
                .findFirst()
                .orElse(drugList.get(0));

        // 从 description 字段中解析详细信息
        String description = drug.getDescription();

        return DrugDetailResponse.builder()
                .id(drug.getId())
                .approvalNumber(drug.getApprovalNumber())
                .genericName(drug.getGenericName())
                .tradeName(drug.getTradeName())
                .commonName(drug.getCommonName())
                .specification(drug.getSpecification())
                .manufacturer(drug.getManufacturer())
                .category(drug.getCategory())
                .ingredient(parseField(description, "成分", "主要成分", "有效成分"))
                .indications(parseField(description, "适应症", "适应症/功能主治", "功能主治"))
                .usage(parseField(description, "用法用量", "用法", "用量"))
                .precautions(parseField(description, "注意事项", "禁忌", "慎用"))
                .adverseReactions(parseField(description, "不良反应", "副作用", "不良反应"))
                .description(description)
                .imageUrl(drug.getImageUrl())
                .build();
    }

    /**
     * 从药品说明原文中解析指定字段
     *
     * @param description 药品说明原文
     * @param keywords    字段关键词（支持多个关键词）
     * @return 解析出的字段内容
     */
    private String parseField(String description, String... keywords) {
        if (description == null || description.isEmpty()) {
            return "暂无详细信息";
        }

        for (String keyword : keywords) {
            // 使用正则表达式匹配字段内容
            // 匹配格式: 关键词：内容（直到下一个关键词或换行）
            String pattern = keyword + "[：:]\\s*([^。；；\\n]+)[。；；\\n]";
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(description);

            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        return "暂无详细信息";
    }

    @Override
    public List<DrugSearchResponse> searchDrugs(String keyword) {
        logger.info("智能搜索药品 - keyword: {}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedKeyword = keyword.trim().toLowerCase();
        List<DrugSearchResponse> results = new ArrayList<>();
        Set<String> seenDrugs = new HashSet<>(); // 去重

        // 1. 先检查是否为类别关键词
        String matchedCategory = matchCategory(normalizedKeyword);
        if (matchedCategory != null) {
            logger.info("匹配到药品类别: {}", matchedCategory);
            List<DrugSearchResponse> categoryResults = searchByCategory(matchedCategory);
            categoryResults.forEach(result -> {
                if (!seenDrugs.contains(result.getDrugName())) {
                    seenDrugs.add(result.getDrugName());
                    results.add(result);
                }
            });
        }

        // 2. 检查是否为别名
        String canonicalName = resolveAlias(normalizedKeyword);
        String searchKeyword = canonicalName != null ? canonicalName : keyword;

        // 3. 数据库模糊搜索
        List<DrugSearchResponse> dbResults = searchFromDatabase(searchKeyword, normalizedKeyword);
        dbResults.forEach(result -> {
            if (!seenDrugs.contains(result.getDrugName())) {
                seenDrugs.add(result.getDrugName());
                results.add(result);
            }
        });

        // 4. 如果数据库没有结果，使用AI搜索作为补充
        if (results.isEmpty()) {
            logger.info("数据库未找到匹配结果，尝试AI搜索");
            return searchDrugsWithAI(keyword);
        }

        // 按匹配度排序
        results.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));

        logger.info("智能搜索完成，找到 {} 个结果", results.size());
        return results;
    }

    @Override
    public List<DrugSearchResponse> searchDrugsWithAI(String keyword) {
        logger.info("使用AI搜索药品 - keyword: {}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 先尝试AI识别
        DrugDetailResponse aiResponse = deepSeekService.queryDrugInfoWithAI(keyword);
        
        if (aiResponse != null && aiResponse.getGenericName() != null && !aiResponse.getGenericName().isEmpty()) {
            logger.info("AI识别成功: {}", aiResponse.getGenericName());
            
            List<DrugSearchResponse> results = new ArrayList<>();
            
            // 创建AI识别结果
            DrugSearchResponse aiResult = DrugSearchResponse.builder()
                    .drugName(aiResponse.getGenericName())
                    .tradeName(aiResponse.getTradeName())
                    .specification(aiResponse.getSpecification())
                    .manufacturer(aiResponse.getManufacturer())
                    .category(aiResponse.getCategory())
                    .matchScore(0.75) // AI识别匹配度
                    .matchType("ai")
                    .build();
            results.add(aiResult);
            
            // 同时在数据库中搜索相关药品
            List<DrugSearchResponse> dbResults = searchFromDatabase(aiResponse.getGenericName(), keyword.toLowerCase());
            dbResults.forEach(result -> {
                if (!result.getDrugName().equals(aiResponse.getGenericName())) {
                    results.add(result);
                }
            });
            
            return results;
        }

        logger.info("AI识别失败，返回空结果");
        return Collections.emptyList();
    }

    /**
     * 匹配药品类别
     */
    private String matchCategory(String keyword) {
        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            for (String categoryKeyword : entry.getValue()) {
                if (keyword.contains(categoryKeyword)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * 解析药品别名
     */
    private String resolveAlias(String keyword) {
        // 精确匹配别名
        if (DRUG_ALIASES.containsKey(keyword)) {
            return DRUG_ALIASES.get(keyword);
        }
        
        // 模糊匹配别名
        for (Map.Entry<String, String> entry : DRUG_ALIASES.entrySet()) {
            if (entry.getKey().contains(keyword) || keyword.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return null;
    }

    /**
     * 根据类别搜索药品
     */
    private List<DrugSearchResponse> searchByCategory(String category) {
        LambdaQueryWrapper<DrugBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(DrugBase::getCategory, category);
        queryWrapper.orderByDesc(DrugBase::getCreatedAt);

        List<DrugBase> drugList = drugBaseMapper.selectList(queryWrapper);
        
        return drugList.stream()
                .map(drug -> DrugSearchResponse.builder()
                        .id(drug.getId())
                        .drugName(drug.getGenericName())
                        .specification(drug.getSpecification())
                        .manufacturer(drug.getManufacturer())
                        .tradeName(drug.getTradeName())
                        .category(drug.getCategory())
                        .matchScore(0.6) // 类别匹配度
                        .matchType("category")
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 从数据库搜索药品
     */
    private List<DrugSearchResponse> searchFromDatabase(String keyword, String normalizedKeyword) {
        LambdaQueryWrapper<DrugBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .like(DrugBase::getGenericName, keyword)
                .or()
                .like(DrugBase::getTradeName, keyword)
                .or()
                .like(DrugBase::getCommonName, keyword)
                .or()
                .like(DrugBase::getManufacturer, keyword));
        queryWrapper.orderByDesc(DrugBase::getCreatedAt);

        List<DrugBase> drugList = drugBaseMapper.selectList(queryWrapper);
        
        return drugList.stream()
                .map(drug -> {
                    double score = calculateMatchScore(drug, normalizedKeyword);
                    return DrugSearchResponse.builder()
                            .id(drug.getId())
                            .drugName(drug.getGenericName())
                            .specification(drug.getSpecification())
                            .manufacturer(drug.getManufacturer())
                            .tradeName(drug.getTradeName())
                            .category(drug.getCategory())
                            .matchScore(score)
                            .matchType(score >= 0.9 ? "exact" : "fuzzy")
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算匹配度分数
     */
    private double calculateMatchScore(DrugBase drug, String keyword) {
        double score = 0.0;
        int matchCount = 0;

        // 通用名精确匹配
        if (drug.getGenericName() != null && drug.getGenericName().equalsIgnoreCase(keyword)) {
            score += 1.0;
            matchCount++;
        } else if (drug.getGenericName() != null && drug.getGenericName().toLowerCase().contains(keyword)) {
            score += 0.8;
            matchCount++;
        }

        // 商品名匹配
        if (drug.getTradeName() != null && drug.getTradeName().toLowerCase().contains(keyword)) {
            score += 0.6;
            matchCount++;
        }

        // 通用名匹配
        if (drug.getCommonName() != null && drug.getCommonName().toLowerCase().contains(keyword)) {
            score += 0.5;
            matchCount++;
        }

        // 厂家匹配
        if (drug.getManufacturer() != null && drug.getManufacturer().toLowerCase().contains(keyword)) {
            score += 0.3;
            matchCount++;
        }

        // 计算平均分
        return matchCount > 0 ? score / matchCount : 0.0;
    }
}
