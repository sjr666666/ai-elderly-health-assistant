package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.model.dto.DrugInfoResponse;
import com.example.backend.model.dto.DrugSearchResponse;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.service.DeepSeekService;
import com.example.backend.service.DrugService;
import com.example.backend.service.rag.RagIngestService;
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
    /** RAG 知识库：新药入库后增量向量化，自动进入用药知识库 */
    private final RagIngestService ragIngestService;

    @Autowired
    public DrugServiceImpl(DrugBaseMapper drugBaseMapper,
                           DeepSeekService deepSeekService,
                           ObjectMapper objectMapper,
                           RagIngestService ragIngestService) {
        this.drugBaseMapper = drugBaseMapper;
        this.deepSeekService = deepSeekService;
        this.objectMapper = objectMapper;
        this.ragIngestService = ragIngestService;
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

        // 第一级：优先查询数据库（稳定、快速、零成本，作为主数据源）
        DrugBase drugInDb = queryDrugFromDatabase(drugName);
        if (drugInDb != null) {
            logger.info("数据库命中药品 - drugName: {}, id: {}", drugInDb.getGenericName(), drugInDb.getId());
            DrugDetailResponse dbResponse = buildDrugDetailFromDatabase(drugName, drugInDb);
            // 关键字段（成分/适应症/用法用量/注意事项/不良反应）任一为空时，再调 AI 补全（数据库优先、AI补充）
            if (isDrugDetailComplete(dbResponse)) {
                logger.info("数据库字段完整，直接返回 - drugName: {}", drugInDb.getGenericName());
                return dbResponse;
            }
            logger.info("数据库字段不完整，调用 AI 补全缺失字段 - drugName: {}", drugInDb.getGenericName());
            return enrichDrugDetailWithAI(drugName, dbResponse);
        }

        // 第二级：数据库没有 → 调 AI 兜底（确保 AI 崩了/无网络时仍能返回友好提示，而不是空白）
        logger.info("数据库未找到药品，尝试 AI 兜底 - drugName: {}", drugName);
        try {
            logger.info("即将调用 AI - 注入的 deepSeekService 类型: {}", deepSeekService.getClass().getName());
            DrugDetailResponse aiResponse = deepSeekService.queryDrugInfoWithAI(drugName);
            logger.info("AI 返回结果: null={}, genericName={}", aiResponse == null,
                    aiResponse == null ? "(null)" : aiResponse.getGenericName());
            if (aiResponse != null && aiResponse.getGenericName() != null && !aiResponse.getGenericName().isEmpty()) {
                logger.info("成功从DeepSeek AI获取药品信息 - {}", aiResponse.getGenericName());
                
                // ✅ 自动保存到数据库，下次查询可直接从数据库获取
                saveDrugToDatabase(aiResponse);
                
                return fillEmptyFields(aiResponse);
            }
            logger.info("DeepSeek AI未返回有效药品信息，回退到友好 fallback");
        } catch (Exception e) {
            logger.warn("DeepSeek AI查询异常: {}，回退到友好 fallback", e.getMessage());
        }

        // 第三级：数据库没有、AI 也没有 → 返回基于 drugName 的友好 fallback 响应
        return buildFallbackDrugDetail(drugName);
    }

    /**
     * 将 AI 返回的药品信息保存到数据库
     */
    private void saveDrugToDatabase(DrugDetailResponse drugDetail) {
        if (drugDetail == null || drugDetail.getGenericName() == null) {
            return;
        }
        try {
            // 先检查是否已存在（避免重复插入）
            DrugBase existing = queryDrugFromDatabase(drugDetail.getGenericName());
            if (existing != null) {
                logger.info("药品已存在于数据库，跳过保存 - drugName: {}", drugDetail.getGenericName());
                return;
            }

            // 构建 description 字段
            StringBuilder description = new StringBuilder();
            if (drugDetail.getIngredient() != null && !drugDetail.getIngredient().isEmpty()) {
                description.append("成分：").append(drugDetail.getIngredient()).append("。");
            }
            if (drugDetail.getIndications() != null && !drugDetail.getIndications().isEmpty()) {
                description.append("适应症：").append(drugDetail.getIndications()).append("。");
            }
            if (drugDetail.getUsage() != null && !drugDetail.getUsage().isEmpty()) {
                description.append("用法用量：").append(drugDetail.getUsage()).append("。");
            }
            if (drugDetail.getPrecautions() != null && !drugDetail.getPrecautions().isEmpty()) {
                description.append("注意事项：").append(drugDetail.getPrecautions()).append("。");
            }
            if (drugDetail.getAdverseReactions() != null && !drugDetail.getAdverseReactions().isEmpty()) {
                description.append("不良反应：").append(drugDetail.getAdverseReactions()).append("。");
            }

            DrugBase drugBase = DrugBase.builder()
                    .genericName(drugDetail.getGenericName())
                    .tradeName(drugDetail.getTradeName())
                    .commonName(null)
                    .specification(drugDetail.getSpecification())
                    .manufacturer(drugDetail.getManufacturer())
                    .category(drugDetail.getCategory())
                    .description(description.length() > 0 ? description.toString() : null)
                    .build();

            int insertCount = drugBaseMapper.insert(drugBase);
            if (insertCount > 0) {
                logger.info("✅ AI药品信息已成功保存到数据库 - drugName: {}, id: {}", 
                    drugDetail.getGenericName(), drugBase.getId());
                // RAG 增量入库：新药知识自动向量化进知识库（失败不阻塞主流程）
                try {
                    ragIngestService.ingestDrug(drugBase.getId());
                } catch (Exception ragEx) {
                    logger.warn("⚠️ 药品增量入知识库失败（不影响主流程）- {}", ragEx.getMessage());
                }
            } else {
                logger.warn("❌ 保存药品到数据库失败 - drugName: {}", drugDetail.getGenericName());
            }
        } catch (Exception e) {
            logger.error("❌ 保存药品到数据库异常 - drugName: {}, error: {}", 
                drugDetail.getGenericName(), e.getMessage(), e);
        }
    }

    /**
     * 从数据库查询药品（按通用名/商品名/俗名模糊匹配）
     */
    private DrugBase queryDrugFromDatabase(String drugName) {
        LambdaQueryWrapper<DrugBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .like(DrugBase::getGenericName, drugName)
                .or()
                .like(DrugBase::getTradeName, drugName)
                .or()
                .like(DrugBase::getCommonName, drugName)
        );
        queryWrapper.orderByDesc(DrugBase::getGenericName);

        List<DrugBase> drugList = drugBaseMapper.selectList(queryWrapper);
        if (drugList == null || drugList.isEmpty()) {
            logger.warn("数据库未找到药品: {}", drugName);
            return null;
        }
        // 优先返回通用名精确匹配的记录
        return drugList.stream()
                .filter(d -> d.getGenericName() != null && d.getGenericName().equals(drugName))
                .findFirst()
                .orElse(drugList.get(0));
    }

    /**
     * 根据数据库实体构建 DrugDetailResponse
     */
    private DrugDetailResponse buildDrugDetailFromDatabase(String drugName, DrugBase drug) {
        String description = drug.getDescription();
        boolean descriptionOk = description != null && !description.trim().isEmpty();

        logger.info("开始解析药品详情 - drugName: {}, description长度: {}", drugName, description != null ? description.length() : 0);
        if (descriptionOk) {
            logger.info("description前100字符: {}", description.substring(0, Math.min(100, description.length())));
        }

        String ingredient = parseFieldOrFallback(description, "成分",
                "该药品具体成分信息请以药品说明书或医生处方为准");
        String indications = parseFieldOrFallback(description, "适应症",
                buildFallbackIndications(drugName, drug.getCategory()));
        String usage = parseFieldOrFallback(description, "用法用量",
                "请遵医嘱或按药品说明书服用，通常为口服，一日 2-3 次");
        String precautions = parseFieldOrFallback(description, "注意事项",
                "用药前请仔细阅读药品说明书，如有过敏史、孕妇、哺乳期妇女、肝肾功能不全者请咨询医生或药师");
        String adverseReactions = parseFieldOrFallback(description, "不良反应",
                "如有皮疹、恶心、头晕等不适请及时停药并咨询医生或药师");

        logger.info("解析结果 - ingredient: {}, indications: {}, usage: {}", 
            ingredient != null ? ingredient.substring(0, Math.min(20, ingredient.length())) : "null",
            indications != null ? indications.substring(0, Math.min(20, indications.length())) : "null",
            usage != null ? usage.substring(0, Math.min(20, usage.length())) : "null");

        return DrugDetailResponse.builder()
                .id(drug.getId())
                .approvalNumber(drug.getApprovalNumber())
                .genericName(drug.getGenericName() != null ? drug.getGenericName() : drugName)
                .tradeName(drug.getTradeName())
                .commonName(drug.getCommonName())
                .specification(drug.getSpecification())
                .manufacturer(drug.getManufacturer())
                .category(drug.getCategory())
                .ingredient(ingredient)
                .indications(indications)
                .usage(usage)
                .precautions(precautions)
                .adverseReactions(adverseReactions)
                .description(descriptionOk ? description :
                        buildFallbackDescription(drugName, drug.getCategory(), drug.getSpecification()))
                .imageUrl(drug.getImageUrl())
                .build();
    }

    /**
     * 判断关键字段是否都完整（都非空且非兜底文案）
     */
    private boolean isDrugDetailComplete(DrugDetailResponse response) {
        if (response == null) return false;
        String[] fields = {
                response.getIngredient(),
                response.getIndications(),
                response.getUsage(),
                response.getPrecautions(),
                response.getAdverseReactions()
        };
        // 只要有任何一个字段是空值或兜底文案，就认为不完整
        for (String f : fields) {
            if (f == null || f.trim().isEmpty()) {
                logger.info("字段为空，认为不完整");
                return false;
            }
            if (isFallbackText(f)) {
                logger.info("字段包含兜底文案，认为不完整: {}", f.substring(0, Math.min(20, f.length())));
                return false;
            }
        }
        logger.info("所有字段完整");
        return true;
    }

    /**
     * 用 AI 补全数据库返回的 DrugDetailResponse 中缺失的字段（数据库已有字段优先保留）
     */
    private DrugDetailResponse enrichDrugDetailWithAI(String drugName, DrugDetailResponse dbResponse) {
        try {
            DrugDetailResponse aiResponse = deepSeekService.queryDrugInfoWithAI(drugName);
            // AI 返回有效信息的标准：genericName 非空且不是兜底文案
            if (aiResponse == null || isBlank(aiResponse.getGenericName()) || aiResponse.getGenericName().contains("暂无") || aiResponse.getGenericName().contains("无法获取")) {
                logger.info("AI 未返回有效信息，保留数据库结果 - drugName: {}", drugName);
                return fillEmptyFields(dbResponse);
            }
            // AI 返回的字段只在数据库字段为空或为兜底文案时替换（数据库优先）
            if (isFallbackText(dbResponse.getIngredient())) dbResponse.setIngredient(aiResponse.getIngredient());
            if (isFallbackText(dbResponse.getIndications())) dbResponse.setIndications(aiResponse.getIndications());
            if (isFallbackText(dbResponse.getUsage())) dbResponse.setUsage(aiResponse.getUsage());
            if (isFallbackText(dbResponse.getPrecautions())) dbResponse.setPrecautions(aiResponse.getPrecautions());
            if (isFallbackText(dbResponse.getAdverseReactions())) dbResponse.setAdverseReactions(aiResponse.getAdverseReactions());
            if (isBlank(dbResponse.getDescription())) dbResponse.setDescription(aiResponse.getDescription());
            logger.info("AI 补全完成 - drugName: {}, ingredient: {}", drugName, aiResponse.getIngredient());
            return fillEmptyFields(dbResponse);
        } catch (Exception e) {
            logger.warn("AI 补全异常: {}，返回数据库结果 - drugName: {}", e.getMessage(), drugName);
            return fillEmptyFields(dbResponse);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 判断字段值是否为兜底文案（非真实药品信息）
     */
    private boolean isFallbackText(String text) {
        if (text == null || text.trim().isEmpty()) return true;
        // 兜底文案关键词（覆盖所有兜底场景的标志性短语）
        String[] fallbackKeywords = {
            // DeepSeek 兜底关键词
            "暂无", "无法获取",
            // buildDrugDetailFromDatabase 兜底关键词
            "请以说明书", "以医生处方为准", "请遵医嘱", "请以药品说明书",
            "请仔细阅读药品说明书", "如有皮疹、恶心", "如有不适请及时",
            "该药品具体", "或按药品说明书", "该药品具体用途",
            // fillEmptyFields 兜底关键词
            "暂无详细成分", "暂无详细适应症", "暂无详细",
            // 通用兜底
            "请以药品说明书或医生处方为准"
        };
        for (String keyword : fallbackKeywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把响应里的空字段填充成"以说明书为准"等友好提示，避免前端展示成空白
     */
    private DrugDetailResponse fillEmptyFields(DrugDetailResponse response) {
        if (response == null) return null;
        if (isBlank(response.getIngredient())) {
            response.setIngredient("暂无详细成分说明");
        }
        if (isBlank(response.getIndications())) {
            response.setIndications("暂无详细适应症说明");
        }
        if (isBlank(response.getUsage())) {
            response.setUsage("请遵医嘱或按药品说明书使用");
        }
        if (isBlank(response.getPrecautions())) {
            response.setPrecautions("请仔细阅读药品说明书，或遵医嘱");
        }
        if (isBlank(response.getAdverseReactions())) {
            response.setAdverseReactions("如有不适请及时咨询医生或药师");
        }
        return response;
    }

    /**
     * 数据库完全没找到药品时，返回友好的 fallback 药品信息，不让用户看到"暂无详细信息"。
     */
    private DrugDetailResponse buildFallbackDrugDetail(String drugName) {
        return DrugDetailResponse.builder()
                .genericName(drugName)
                .tradeName(drugName)
                .ingredient("请以药品说明书为准")
                .indications("该药品具体适应症请以药品说明书或医生处方为准")
                .usage("请遵医嘱或按药品说明书服用")
                .precautions("用药前请仔细阅读药品说明书，如有不适请咨询医生或药师")
                .adverseReactions("如有不适请及时停药并咨询医生或药师")
                .description("通用药品信息，具体请以药品说明书为准")
                .build();
    }

    /**
     * 根据药品类别生成 fallback 适应症说明，比固定的"暂无详细信息"更有帮助
     */
    private String buildFallbackIndications(String drugName, String category) {
        if (category == null || category.isEmpty()) {
            return "该药品具体用途请以药品说明书或医生处方为准，或咨询您的医生/药师";
        }
        String cat = category.trim();
        if (cat.contains("感冒")) {
            return "缓解普通感冒或流行性感冒引起的发热、头痛、鼻塞、咽痛等症状";
        }
        if (cat.contains("止痛") || cat.contains("退烧")) {
            return "用于缓解轻至中度疼痛，如头痛、关节痛、牙痛、肌肉痛等，也可用于退热";
        }
        if (cat.contains("消炎") || cat.contains("抗生素")) {
            return "用于敏感菌引起的呼吸道、泌尿道、皮肤软组织等感染，具体请遵医嘱";
        }
        if (cat.contains("胃")) {
            return "用于缓解胃酸过多、胃痛、胃胀、消化不良等胃肠道不适";
        }
        if (cat.contains("降压")) {
            return "用于高血压的治疗，请遵医嘱按时服药，定期监测血压";
        }
        if (cat.contains("降糖")) {
            return "用于 2 型糖尿病的血糖控制，配合饮食运动，请遵医嘱";
        }
        if (cat.contains("抗过敏")) {
            return "用于过敏性鼻炎、荨麻疹、皮肤瘙痒等过敏性疾病的缓解";
        }
        if (cat.contains("心脏") || cat.contains("心血管")) {
            return "用于冠心病、心绞痛、高血压等心血管疾病的治疗或预防，请遵医嘱";
        }
        return "该药品具体用途请以药品说明书或医生处方为准";
    }

    /**
     * 生成 description 字段的 fallback 文本
     */
    private String buildFallbackDescription(String drugName, String category, String spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("药品名称：").append(drugName);
        if (category != null && !category.isEmpty()) sb.append("；类别：").append(category);
        if (spec != null && !spec.isEmpty()) sb.append("；规格：").append(spec);
        sb.append("。详细用药信息请以药品说明书或医生处方为准。");
        return sb.toString();
    }

    /**
     * 从 description 中智能解析字段，失败则返回 fallbackText
     * （比原来的"暂无详细信息"更有帮助，也避免前端看到大片空白）
     */
    private String parseFieldOrFallback(String description, String fieldType, String fallbackText) {
        String parsed = smartParseField(description, fieldType);
        if (parsed == null || parsed.isEmpty() || "暂无详细信息".equals(parsed)) {
            return fallbackText;
        }
        return parsed;
    }

    /**
     * 智能解析 description 文本中的字段。
     * 支持多种写法："成分：xxx", "主要成分：xxx", "【成分】xxx", "成分 xxx" 等。
     */
    private String smartParseField(String description, String fieldType) {
        if (description == null || description.trim().isEmpty()) {
            return "暂无详细信息";
        }
        String desc = description.trim();

        // 为每个字段类型准备一组常见关键词（含中文全角冒号、英文冒号、【】等写法）
        String[][] keywordGroups;
        switch (fieldType) {
            case "成分":
                keywordGroups = new String[][]{
                        {"成分", "主要成分", "有效成分", "活性成分", "成份", "主要成份"},
                        {"本品", "本品为", "组分为"}
                };
                break;
            case "适应症":
                keywordGroups = new String[][]{
                        {"适应症", "适应症/功能主治", "功能主治", "适用于", "用于"},
                        {"主治", "治"},
                        {"功效", "作用"}
                };
                break;
            case "用法用量":
                keywordGroups = new String[][]{
                        {"用法用量", "用法", "用量", "口服", "服用方法"},
                        {"一次", "一日", "每次", "每日"}
                };
                break;
            case "注意事项":
                keywordGroups = new String[][]{
                        {"注意事项", "禁忌", "慎用", "禁用", "忌用", "注意"},
                        {"孕妇", "哺乳期", "儿童", "老年", "过敏"}
                };
                break;
            case "不良反应":
                keywordGroups = new String[][]{
                        {"不良反应", "副作用", "副反应", "可能引起"},
                        {"偶见", "可见", "少见", "常见"}
                };
                break;
            default:
                keywordGroups = new String[][]{{fieldType}};
        }

        // 第一轮：精确匹配 "关键词：内容" 形式（支持全角/半角冒号、空格）
        for (String[] group : keywordGroups) {
            for (String keyword : group) {
                String result = tryExtractByKeyword(desc, keyword);
                if (result != null && !result.isEmpty()) {
                    return result;
                }
            }
        }

        // 没有找到关键词，返回空标记让上层使用 fallback
        return "暂无详细信息";
    }

    /**
     * 尝试从 description 中提取 "keyword：内容" 格式的字段值
     * 内容截止到下一个字段的关键词（如 "用法用量" / "不良反应" 等）。
     * 注意：不再按单个 "。" 截断，因为"注意事项"等字段本身可能跨越多个句子（如 "1. ... 2. ... 3. ..."）。
     */
    private String tryExtractByKeyword(String description, String keyword) {
        int idx = description.indexOf(keyword);
        if (idx < 0) return null;

        int start = idx + keyword.length();
        int len = description.length();

        // 跳过紧随其后的冒号/空格/：等分隔符
        while (start < len) {
            char c = description.charAt(start);
            if (c == ':' || c == '：' || c == ' ' || c == '\t' || c == '\n') {
                start++;
                continue;
            }
            break;
        }
        if (start >= len) return null;

        // 寻找内容结束位置：下一个字段的关键词
        String[] nextStopKeywords = {"用法用量", "适应症", "成分", "注意事项", "不良反应", "规格", "禁忌", "功能主治", "性状", "贮藏", "包装"};
        int endPos = len;
        for (String next : nextStopKeywords) {
            // 跳过 keyword 自身（防止和当前 keyword 的位置比较）
            if (next.equals(keyword)) continue;
            int nextIdx = description.indexOf(next, start);
            if (nextIdx > start && nextIdx < endPos) {
                endPos = nextIdx;
            }
        }

        // 限制最大长度，避免返回过长文本
        int maxLen = 500;
        if (endPos - start > maxLen) {
            endPos = start + maxLen;
        }

        String value = description.substring(start, endPos).trim();

        // 清除尾部多余的分隔符/标点
        while (!value.isEmpty() && (value.endsWith("，") || value.endsWith(",") || value.endsWith("：") || value.endsWith(":")
                || value.endsWith("。") || value.endsWith("；") || value.endsWith(";"))) {
            value = value.substring(0, value.length() - 1).trim();
        }

        if (value.isEmpty() || value.length() < 2) return null;
        return value;
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
