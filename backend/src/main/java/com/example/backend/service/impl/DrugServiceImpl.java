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
        CATEGORY_KEYWORDS.put("跌打损伤药", Arrays.asList("摔伤", "跌打", "扭伤", "撞伤", "骨折", "外伤", "瘀血", "肿痛", "活血", "化瘀"));
    }

    // 常见药品别名映射
    private static final Map<String, String> DRUG_ALIASES = new HashMap<>();
    static {
        DRUG_ALIASES.put("扑热息痛", "对乙酰氨基酚");
        DRUG_ALIASES.put("泰诺", "对乙酰氨基酚");
        DRUG_ALIASES.put("泰诺林", "对乙酰氨基酚");
        DRUG_ALIASES.put("百服宁", "对乙酰氨基酚");
        DRUG_ALIASES.put("必理通", "对乙酰氨基酚");
        // 布洛芬
        DRUG_ALIASES.put("芬必得", "布洛芬");
        DRUG_ALIASES.put("美林", "布洛芬");
        DRUG_ALIASES.put("安瑞克", "布洛芬");
        DRUG_ALIASES.put("芬必得布洛芬", "布洛芬");
        // 阿司匹林
        DRUG_ALIASES.put("乙酰水杨酸", "阿司匹林");
        DRUG_ALIASES.put("拜阿司匹林", "阿司匹林");
        DRUG_ALIASES.put("拜阿司匹灵", "阿司匹林");
        // 硝苯地平
        DRUG_ALIASES.put("心痛定", "硝苯地平");
        DRUG_ALIASES.put("拜新同", "硝苯地平");
        DRUG_ALIASES.put("伲福达", "硝苯地平");
        // 二甲双胍
        DRUG_ALIASES.put("格华止", "二甲双胍");
        DRUG_ALIASES.put("盐酸二甲双胍", "二甲双胍");
        // 阿莫西林
        DRUG_ALIASES.put("阿莫仙", "阿莫西林");
        DRUG_ALIASES.put("安必仙", "阿莫西林");
        DRUG_ALIASES.put("阿莫灵", "阿莫西林");
        // 头孢克肟
        DRUG_ALIASES.put("世福素", "头孢克肟");
        DRUG_ALIASES.put("达力芬", "头孢克肟");
        // 蒙脱石散
        DRUG_ALIASES.put("思密达", "蒙脱石散");
        DRUG_ALIASES.put("蒙脱石", "蒙脱石散");
        // 奥美拉唑
        DRUG_ALIASES.put("洛赛克", "奥美拉唑");
        DRUG_ALIASES.put("奥克", "奥美拉唑");
        DRUG_ALIASES.put("奥美", "奥美拉唑");
        // 氯雷他定
        DRUG_ALIASES.put("开瑞坦", "氯雷他定");
        DRUG_ALIASES.put("息斯敏", "氯雷他定");
        DRUG_ALIASES.put("雷诺敏", "氯雷他定");
        // 西替利嗪
        DRUG_ALIASES.put("仙特明", "盐酸西替利嗪");
        DRUG_ALIASES.put("西可韦", "盐酸西替利嗪");
        // 氨氯地平
        DRUG_ALIASES.put("络活喜", "苯磺酸氨氯地平");
        DRUG_ALIASES.put("安内真", "苯磺酸氨氯地平");
        // 氨溴索
        DRUG_ALIASES.put("沐舒坦", "盐酸氨溴索");
        DRUG_ALIASES.put("氨溴索", "盐酸氨溴索");
        // 云南白药
        DRUG_ALIASES.put("白药", "云南白药");
        DRUG_ALIASES.put("白药气雾剂", "云南白药气雾剂");
        // 藿香正气
        DRUG_ALIASES.put("藿香正气", "藿香正气水");
        DRUG_ALIASES.put("霍香正气", "藿香正气水");
        // 板蓝根
        DRUG_ALIASES.put("板兰根", "板蓝根");
        // 葡萄糖酸钙
        DRUG_ALIASES.put("钙片", "葡萄糖酸钙片");
        DRUG_ALIASES.put("钙尔奇", "钙尔奇D片");
        // 阿卡波糖
        DRUG_ALIASES.put("拜糖平", "阿卡波糖");
        // 格列齐特
        DRUG_ALIASES.put("达美康", "格列齐特");
        // 辛伐他汀
        DRUG_ALIASES.put("舒降之", "辛伐他汀");
        // 氯吡格雷
        DRUG_ALIASES.put("波立维", "硫酸氢氯吡格雷");
        // 阿托伐他汀
        DRUG_ALIASES.put("立普妥", "阿托伐他汀钙");
        // 丹参滴丸
        DRUG_ALIASES.put("丹参滴丸", "复方丹参滴丸");
        // 银杏叶
        DRUG_ALIASES.put("银杏叶", "银杏叶片");
        // 甲钴胺
        DRUG_ALIASES.put("甲钴胺", "甲钴胺片");
        DRUG_ALIASES.put("弥可保", "甲钴胺片");
        // 多潘立酮
        DRUG_ALIASES.put("吗丁啉", "多潘立酮");
        // 泮托拉唑
        DRUG_ALIASES.put("泮托拉唑", "泮托拉唑钠");
        DRUG_ALIASES.put("潘妥洛克", "泮托拉唑钠");
        // 左氧氟沙星
        DRUG_ALIASES.put("可乐必妥", "左氧氟沙星");
        DRUG_ALIASES.put("左克", "左氧氟沙星");
        // 阿奇霉素
        DRUG_ALIASES.put("希舒美", "阿奇霉素");
        // 罗红霉素
        DRUG_ALIASES.put("罗红霉素", "罗红霉素");
        DRUG_ALIASES.put("仁苏", "罗红霉素");
        // 甲硝唑
        DRUG_ALIASES.put("甲硝唑", "甲硝唑");
        DRUG_ALIASES.put("灭滴灵", "甲硝唑");
        // 替硝唑
        DRUG_ALIASES.put("替硝唑", "替硝唑");
        // 克霉唑
        DRUG_ALIASES.put("克霉唑", "克霉唑");
        DRUG_ALIASES.put("凯妮汀", "克霉唑");
        // 咪康唑
        DRUG_ALIASES.put("咪康唑", "咪康唑");
        DRUG_ALIASES.put("达克宁", "咪康唑");
        // 特比萘芬
        DRUG_ALIASES.put("特比萘芬", "特比萘芬");
        DRUG_ALIASES.put("兰美抒", "特比萘芬");
        // 炉甘石
        DRUG_ALIASES.put("炉甘石", "炉甘石洗剂");
        // 碘伏
        DRUG_ALIASES.put("碘伏", "碘伏");
        DRUG_ALIASES.put("碘酒", "碘伏");
        // 酒精
        DRUG_ALIASES.put("酒精", "乙醇");
        // 生理盐水
        DRUG_ALIASES.put("生理盐水", "氯化钠注射液");
        // 葡萄糖
        DRUG_ALIASES.put("葡萄糖", "葡萄糖注射液");
        // 氯化钾
        DRUG_ALIASES.put("氯化钾", "氯化钾");
        // 维生素C
        DRUG_ALIASES.put("维C", "维生素C");
        DRUG_ALIASES.put("维生素C", "维生素C片");
        // 维生素B
        DRUG_ALIASES.put("维B", "复合维生素B");
        DRUG_ALIASES.put("B族", "复合维生素B");
        // 褪黑素
        DRUG_ALIASES.put("褪黑素", "褪黑素片");
        DRUG_ALIASES.put("脑白金", "褪黑素片");
        // 安神补脑
        DRUG_ALIASES.put("安神补脑", "安神补脑液");
        // 养血安神
        DRUG_ALIASES.put("养血安神", "养血安神片");
        // 红花油
        DRUG_ALIASES.put("红花油", "红花油");
        DRUG_ALIASES.put("正红花油", "红花油");
        // 正骨水
        DRUG_ALIASES.put("正骨水", "正骨水");
        // 扶他林
        DRUG_ALIASES.put("扶他林", "双氯芬酸二乙胺");
        DRUG_ALIASES.put("双氯芬酸", "双氯芬酸钠");
        // 派瑞松
        DRUG_ALIASES.put("派瑞松", "曲安奈德益康唑");
        // 百多邦
        DRUG_ALIASES.put("百多邦", "莫匹罗星");
        // 皮炎平
        DRUG_ALIASES.put("皮炎平", "复方醋酸地塞米松");
        // 珍视明
        DRUG_ALIASES.put("珍视明", "四味珍层冰硼滴眼液");
        DRUG_ALIASES.put("珍珠明目", "珍珠明目滴眼液");
        // 氯霉素
        DRUG_ALIASES.put("氯霉素", "氯霉素滴眼液");
        // 玻璃酸钠
        DRUG_ALIASES.put("玻璃酸钠", "玻璃酸钠滴眼液");
        DRUG_ALIASES.put("海露", "玻璃酸钠滴眼液");
        // 茶苯海明
        DRUG_ALIASES.put("茶苯海明", "茶苯海明片");
        DRUG_ALIASES.put("乘晕宁", "茶苯海明片");
        // 地芬尼多
        DRUG_ALIASES.put("地芬尼多", "盐酸地芬尼多");
        DRUG_ALIASES.put("眩晕停", "盐酸地芬尼多");
        // 清开灵
        DRUG_ALIASES.put("清开灵", "清开灵颗粒");
        // 小儿氨酚黄那敏
        DRUG_ALIASES.put("小儿氨酚黄那敏", "小儿氨酚黄那敏颗粒");
        DRUG_ALIASES.put("小快克", "小儿氨酚黄那敏颗粒");
        // 小儿肺热咳喘
        DRUG_ALIASES.put("小儿肺热咳喘", "小儿肺热咳喘口服液");
        DRUG_ALIASES.put("葵花肺热咳喘", "小儿肺热咳喘口服液");
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

        // 最小搜索长度限制
        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.length() < 2) {
            logger.info("搜索关键词太短（少于2个字符），返回空结果");
            return Collections.emptyList();
        }

        String normalizedKeyword = trimmedKeyword.toLowerCase();
        List<DrugSearchResponse> results = new ArrayList<>();
        Set<String> seenDrugs = new HashSet<>(); // 去重

        // 1. 先检查是否为类别关键词（要求完整匹配或至少包含完整的关键词）
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
        
        // 3. 数据库模糊搜索 - 同时使用原始关键词和标准名称进行搜索
        List<DrugSearchResponse> dbResults = new ArrayList<>();
        
        // 先使用原始关键词搜索
        List<DrugSearchResponse> originalResults = searchFromDatabase(keyword, normalizedKeyword);
        logger.info("使用原始关键词'{}'搜索到 {} 条结果", keyword, originalResults.size());
        if (!originalResults.isEmpty()) {
            originalResults.forEach(r -> logger.info("  - {}: 匹配度={}", r.getDrugName(), r.getMatchScore()));
        }
        dbResults.addAll(originalResults);
        
        // 如果有标准名称且与原始关键词不同，也使用标准名称搜索
        if (canonicalName != null && !canonicalName.equals(keyword)) {
            List<DrugSearchResponse> canonicalResults = searchFromDatabase(canonicalName, canonicalName.toLowerCase());
            // 合并结果，去重
            canonicalResults.forEach(result -> {
                if (!dbResults.stream().anyMatch(r -> r.getDrugName().equals(result.getDrugName()))) {
                    dbResults.add(result);
                }
            });
        }
        dbResults.forEach(result -> {
            if (!seenDrugs.contains(result.getDrugName())) {
                seenDrugs.add(result.getDrugName());
                results.add(result);
            }
        });

        // 4. 过滤掉匹配度太低的结果（只保留匹配度>=0.3的结果）
        logger.info("过滤前结果数量: {}", results.size());
        List<DrugSearchResponse> filteredResults = results.stream()
                .filter(r -> r.getMatchScore() == null || r.getMatchScore() >= 0.3)
                .collect(Collectors.toList());
        logger.info("过滤后结果数量: {}", filteredResults.size());

        // 5. 如果数据库没有结果，使用AI搜索作为补充
        if (filteredResults.isEmpty()) {
            logger.info("数据库未找到匹配结果，尝试AI搜索");
            return searchDrugsWithAI(keyword);
        }

        // 按匹配度排序
        filteredResults.sort((a, b) -> Double.compare(
                b.getMatchScore() != null ? b.getMatchScore() : 0, 
                a.getMatchScore() != null ? a.getMatchScore() : 0));

        logger.info("智能搜索完成，找到 {} 个结果", filteredResults.size());
        return filteredResults;
    }

    @Override
    public List<DrugSearchResponse> searchDrugsWithAI(String keyword) {
        logger.info("使用AI搜索药品 - keyword: {}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 最小搜索长度限制：单字搜索优先使用数据库
        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.length() < 1) {
            logger.info("AI搜索关键词为空，返回空结果");
            return Collections.emptyList();
        }

        // 单字搜索：优先使用数据库，不调用AI（避免偏差大）
        if (trimmedKeyword.length() == 1) {
            logger.info("单字搜索，优先使用数据库匹配 - keyword: {}", keyword);
            List<DrugSearchResponse> dbResults = searchFromDatabase(keyword, keyword.toLowerCase());
            if (!dbResults.isEmpty()) {
                logger.info("数据库搜索成功，找到 {} 个相关药品", dbResults.size());
                return dbResults;
            }
            // 数据库没结果才调用AI
            logger.info("数据库无结果，尝试AI搜索单字");
        }

        // 使用AI搜索多个相关药品
        List<DrugSearchResponse> aiResults = deepSeekService.searchMultipleDrugsWithAI(keyword);
        
        if (aiResults != null && !aiResults.isEmpty()) {
            logger.info("AI搜索成功，找到 {} 个相关药品", aiResults.size());
            
            // 合并数据库中的结果
            List<DrugSearchResponse> allResults = new ArrayList<>(aiResults);
            
            // 在数据库中搜索每个AI返回的药品，补充完整信息
            for (DrugSearchResponse aiResult : aiResults) {
                List<DrugSearchResponse> dbResults = searchFromDatabase(aiResult.getDrugName(), keyword.toLowerCase());
                dbResults.forEach(result -> {
                    // 如果数据库中已有该药品，更新AI结果的信息
                    if (result.getDrugName().equals(aiResult.getDrugName())) {
                        aiResult.setId(result.getId());
                        aiResult.setSpecification(result.getSpecification() != null ? result.getSpecification() : aiResult.getSpecification());
                        aiResult.setManufacturer(result.getManufacturer() != null ? result.getManufacturer() : aiResult.getManufacturer());
                        aiResult.setCategory(result.getCategory() != null ? result.getCategory() : aiResult.getCategory());
                        aiResult.setMatchScore(Math.max(result.getMatchScore(), aiResult.getMatchScore()));
                        aiResult.setMatchType("ai+db");
                    }
                });
            }
            
            return allResults;
        }

        logger.info("AI搜索失败，返回空结果");
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
        logger.info("执行数据库搜索 - keyword: {}, normalizedKeyword: {}", keyword, normalizedKeyword);
        
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

        // 打印生成的SQL（用于调试）
        logger.info("查询条件 - generic_name LIKE '%{}%' OR trade_name LIKE '%{}%' OR common_name LIKE '%{}%' OR manufacturer LIKE '%{}%'",
                keyword, keyword, keyword, keyword);
        
        List<DrugBase> drugList = drugBaseMapper.selectList(queryWrapper);
        logger.info("数据库返回结果数量: {}", drugList.size());
        if (!drugList.isEmpty()) {
            drugList.forEach(drug -> logger.info("  - {}: genericName={}, tradeName={}, commonName={}",
                    drug.getId(), drug.getGenericName(), drug.getTradeName(), drug.getCommonName()));
        }
        
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
