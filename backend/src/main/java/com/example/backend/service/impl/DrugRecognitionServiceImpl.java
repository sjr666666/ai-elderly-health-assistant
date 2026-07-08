package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.util.DrugNameNormalizer;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.OcrRecordMapper;
import com.example.backend.model.dto.OcrResultResponse;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.DrugRecognitionLog;
import com.example.backend.model.entity.OcrRecord;
import com.example.backend.service.DeepSeekService;
import com.example.backend.service.DrugRecognitionLogService;
import com.example.backend.service.DrugRecognitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 药品识别服务实现类
 */
@Service
public class DrugRecognitionServiceImpl implements DrugRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(DrugRecognitionServiceImpl.class);

    /**
     * 匹配阈值 - 高于此阈值认为匹配成功
     */
    private static final BigDecimal MATCH_THRESHOLD = new BigDecimal("0.6");

    /**
     * 自动入库阈值 - 低于匹配阈值但高于此阈值时自动入库
     */
    private static final BigDecimal AUTO_IMPORT_THRESHOLD = new BigDecimal("0.3");

    private final DrugBaseMapper drugBaseMapper;
    private final OcrRecordMapper ocrRecordMapper;
    private final DrugRecognitionLogService logService;
    private final DrugNameNormalizer nameNormalizer;
    private final DeepSeekService deepSeekService;

    @Autowired
    public DrugRecognitionServiceImpl(
            DrugBaseMapper drugBaseMapper,
            OcrRecordMapper ocrRecordMapper,
            DrugRecognitionLogService logService,
            DrugNameNormalizer nameNormalizer,
            DeepSeekService deepSeekService) {
        this.drugBaseMapper = drugBaseMapper;
        this.ocrRecordMapper = ocrRecordMapper;
        this.logService = logService;
        this.nameNormalizer = nameNormalizer;
        this.deepSeekService = deepSeekService;
    }

    @Override
    public OcrResultResponse processRecognition(Long ocrRecordId, String rawText) {
        logger.info("开始处理药品识别 - ocrRecordId: {}, rawText: {}", ocrRecordId, rawText);

        // 获取OCR记录信息
        OcrRecord ocrRecord = ocrRecordMapper.selectById(ocrRecordId);
        if (ocrRecord == null) {
            logger.error("未找到OCR记录 - ocrRecordId: {}", ocrRecordId);
            return null;
        }

        // 创建识别日志
        DrugRecognitionLog recognitionLog = new DrugRecognitionLog();
        recognitionLog.setOcrRecordId(ocrRecordId);
        recognitionLog.setUserId(ocrRecord.getUserId());
        recognitionLog.setRawText(rawText);
        recognitionLog.setStatus(DrugRecognitionLog.Status.PENDING.getCode());
        recognitionLog.setCreatedAt(LocalDateTime.now());
        
        Long logId = null;
        try {
            logService.createLog(recognitionLog);
            logId = recognitionLog.getId();
        } catch (Exception logEx) {
            logger.warn("创建识别日志失败（可能表不存在），将继续识别流程 - error: {}", logEx.getMessage());
        }

        try {
            // 步骤1: 验证原始文本
            if (rawText == null || rawText.trim().isEmpty()) {
                logger.info("识别文本为空 - ocrRecordId: {}", ocrRecordId);
                if (logId != null) {
                    logService.updateUnmatched(logId, "识别文本为空");
                }
                return buildResult(null, null, null, "unmatched", "未能识别出文字");
            }

            // 步骤2: 从OCR文本中提取核心药品名称（过滤说明书内容）
            String extractedName = nameNormalizer.extractDrugName(rawText);
            
            // 如果本地提取失败，尝试使用DeepSeek AI进行识别
            if (extractedName == null || extractedName.trim().isEmpty()) {
                logger.info("本地提取药品名称失败，尝试调用DeepSeek AI - ocrRecordId: {}", ocrRecordId);
                extractedName = deepSeekService.extractDrugNameWithAI(rawText);
            }
            
            if (extractedName == null || extractedName.trim().isEmpty()) {
                logger.info("未能从识别文本中提取药品名称（本地和AI都失败） - ocrRecordId: {}", ocrRecordId);
                if (logId != null) {
                    logService.updateUnmatched(logId, "未能提取药品名称");
                }
                return buildResult(null, null, null, "unmatched", "未能识别出药品名称");
            }

            logger.info("提取药品名称成功 - 提取结果: {}", extractedName);

            // 步骤3: 标准化药品名称
            String normalizedName = nameNormalizer.normalize(extractedName);
            recognitionLog.setNormalizedName(normalizedName);
            if (logId != null) {
                try {
                    logService.createLog(recognitionLog);
                } catch (Exception logEx) {
                    logger.warn("更新识别日志失败 - error: {}", logEx.getMessage());
                }
            }

            logger.info("药品名称标准化完成 - 提取: {}, 标准化后: {}", extractedName, normalizedName);

            // 步骤3: 验证标准化后的名称
            ValidationResult validationResult = validateDrugName(normalizedName);
            if (!validationResult.isValid()) {
                logger.info("药品名称验证失败 - reason: {}", validationResult.getMessage());
                if (logId != null) {
                    logService.updateUnmatched(logId, validationResult.getMessage());
                }
                return buildResult(null, null, null, "unmatched", validationResult.getMessage());
            }

            // 步骤4: 在数据库中查找匹配的药品
            DrugMatchResult matchResult = findMatchingDrug(normalizedName);

            if (matchResult.isMatched()) {
                // 匹配成功
                logger.info("药品匹配成功 - drugId: {}, drugName: {}, score: {}",
                        matchResult.getDrugId(), matchResult.getDrugName(), matchResult.getScore());

                if (logId != null) {
                    try {
                        logService.updateMatched(logId, matchResult.getDrugId(),
                                matchResult.getDrugName(), matchResult.getScore());
                    } catch (Exception logEx) {
                        logger.warn("更新匹配日志失败 - error: {}", logEx.getMessage());
                    }
                }

                return buildResult(matchResult.getDrugId(), matchResult.getDrugName(),
                        matchResult.getScore(), "matched", "药品识别成功");

            } else {
                // 未匹配到现有药品
                logger.info("未匹配到现有药品，尝试自动入库 - normalizedName: {}", normalizedName);

                // 步骤5: 尝试自动入库新药品
                DrugBase newDrug = autoImportNewDrug(normalizedName, rawText);
                
                if (newDrug != null) {
                    // 自动入库成功
                    if (logId != null) {
                        try {
                            logService.updateImported(logId, newDrug.getId(), newDrug.getGenericName());
                        } catch (Exception logEx) {
                            logger.warn("更新入库日志失败 - error: {}", logEx.getMessage());
                        }
                    }
                    logger.info("新药品自动入库成功 - drugId: {}, drugName: {}",
                            newDrug.getId(), newDrug.getGenericName());

                    return buildResult(newDrug.getId(), newDrug.getGenericName(),
                            new BigDecimal("1.0"), "matched", "新药品已自动添加到数据库");
                } else {
                    // 自动入库失败或不满足条件
                    if (logId != null) {
                        logService.updateUnmatched(logId, "未能匹配到现有药品，且不符合自动入库条件");
                    }
                    return buildResult(null, null, null, "unmatched", "未能识别出匹配的药品，请尝试手动输入");
                }
            }

        } catch (Exception e) {
            logger.error("药品识别处理失败 - ocrRecordId: {}", ocrRecordId, e);
            if (logId != null) {
                try {
                    logService.updateUnmatched(logId, "处理异常: " + e.getMessage());
                } catch (Exception logEx) {
                    logger.warn("更新异常日志失败 - error: {}", logEx.getMessage());
                }
            }
            return buildResult(null, null, null, OcrRecord.Status.FAILED.getCode(), "识别失败，请重试");
        }
    }

    @Override
    public String normalizeDrugName(String rawName) {
        return nameNormalizer.normalize(rawName);
    }

    @Override
    public ValidationResult validateDrugName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new ValidationResult(false, "药品名称不能为空", null);
        }

        String normalized = nameNormalizer.normalize(name);
        
        if (!nameNormalizer.isValidDrugName(normalized)) {
            return new ValidationResult(false, "药品名称无效，至少需要包含2个中文字符或4个英文字符", normalized);
        }

        return new ValidationResult(true, "药品名称验证通过", normalized);
    }

    /**
     * 在数据库中查找匹配的药品
     */
    private DrugMatchResult findMatchingDrug(String normalizedName) {
        if (normalizedName == null || normalizedName.isEmpty()) {
            return new DrugMatchResult(false, null, null, BigDecimal.ZERO);
        }

        DrugBase bestMatch = null;
        BigDecimal bestScore = BigDecimal.ZERO;

        // 构建查询条件
        LambdaQueryWrapper<DrugBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .like(DrugBase::getGenericName, normalizedName)
                .or()
                .like(DrugBase::getTradeName, normalizedName)
                .or()
                .like(DrugBase::getCommonName, normalizedName));

        List<DrugBase> drugList = drugBaseMapper.selectList(queryWrapper);

        for (DrugBase drug : drugList) {
            BigDecimal score = calculateMatchScore(normalizedName, drug);
            if (score.compareTo(bestScore) > 0) {
                bestScore = score;
                bestMatch = drug;
            }
        }

        // 检查是否有完全匹配
        if (bestMatch != null) {
            String genericName = bestMatch.getGenericName() != null ?
                    bestMatch.getGenericName().toLowerCase().replaceAll("\\s", "") : "";
            String tradeName = bestMatch.getTradeName() != null ?
                    bestMatch.getTradeName().toLowerCase().replaceAll("\\s", "") : "";
            String normalizedLower = normalizedName.toLowerCase().replaceAll("\\s", "");

            // 如果完全匹配，直接返回最高分数
            if (genericName.equals(normalizedLower) || tradeName.equals(normalizedLower)) {
                logger.info("发现完全匹配的药品 - drugId: {}, drugName: {}", bestMatch.getId(), bestMatch.getGenericName());
                return new DrugMatchResult(true, bestMatch.getId(), bestMatch.getGenericName(), new BigDecimal("1.0"));
            }
        }

        // 根据阈值判断是否匹配成功
        if (bestMatch != null && bestScore.compareTo(MATCH_THRESHOLD) >= 0) {
            return new DrugMatchResult(true, bestMatch.getId(), bestMatch.getGenericName(), bestScore);
        }

        return new DrugMatchResult(false, null, null, bestScore);
    }

    /**
     * 计算匹配分数
     */
    private BigDecimal calculateMatchScore(String searchText, DrugBase drug) {
        double similarity = 0.0;
        int matchCount = 0;
        int totalFields = 0;

        String searchLower = searchText.toLowerCase().replaceAll("\\s", "");

        // 比较通用名
        if (drug.getGenericName() != null && !drug.getGenericName().isEmpty()) {
            String genericLower = drug.getGenericName().toLowerCase().replaceAll("\\s", "");
            similarity += nameNormalizer.calculateSimilarity(searchLower, genericLower) * 0.5;
            if (genericLower.contains(searchLower) || searchLower.contains(genericLower)) {
                matchCount++;
            }
            totalFields++;
        }

        // 比较商品名
        if (drug.getTradeName() != null && !drug.getTradeName().isEmpty()) {
            String tradeLower = drug.getTradeName().toLowerCase().replaceAll("\\s", "");
            similarity += nameNormalizer.calculateSimilarity(searchLower, tradeLower) * 0.3;
            if (tradeLower.contains(searchLower) || searchLower.contains(tradeLower)) {
                matchCount++;
            }
            totalFields++;
        }

        // 比较俗名
        if (drug.getCommonName() != null && !drug.getCommonName().isEmpty()) {
            String commonLower = drug.getCommonName().toLowerCase().replaceAll("\\s", "");
            similarity += nameNormalizer.calculateSimilarity(searchLower, commonLower) * 0.2;
            if (commonLower.contains(searchLower) || searchLower.contains(commonLower)) {
                matchCount++;
            }
            totalFields++;
        }

        // 如果有多个字段匹配，增加额外分数
        if (totalFields > 0 && matchCount == totalFields) {
            similarity += 0.1;
        }

        // 确保分数不超过1.0
        similarity = Math.min(1.0, similarity);

        return BigDecimal.valueOf(similarity).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 自动导入新药品到数据库
     */
    private DrugBase autoImportNewDrug(String normalizedName, String rawText) {
        // 检查是否已经存在相同名称的药品
        LambdaQueryWrapper<DrugBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .eq(DrugBase::getGenericName, normalizedName)
                .or()
                .eq(DrugBase::getTradeName, normalizedName)
                .or()
                .eq(DrugBase::getCommonName, normalizedName));

        List<DrugBase> existingDrugs = drugBaseMapper.selectList(queryWrapper);
        if (!existingDrugs.isEmpty()) {
            logger.info("药品已存在于数据库中 - normalizedName: {}", normalizedName);
            return existingDrugs.get(0);
        }

        try {
            // 调用AI获取药品详细信息
            logger.info("调用AI获取药品详细信息 - drugName: {}", normalizedName);
            com.example.backend.model.dto.DrugDetailResponse aiResponse = deepSeekService.queryDrugInfoWithAI(normalizedName);
            
            if (aiResponse != null && aiResponse.getGenericName() != null && !aiResponse.getGenericName().isEmpty()) {
                logger.info("AI返回成功 - genericName: {}, ingredient: {}", aiResponse.getGenericName(), aiResponse.getIngredient());
                
                // 构建description字段
                StringBuilder description = new StringBuilder();
                if (aiResponse.getIngredient() != null && !aiResponse.getIngredient().isEmpty()) {
                    description.append("成分：").append(aiResponse.getIngredient()).append("。");
                }
                if (aiResponse.getIndications() != null && !aiResponse.getIndications().isEmpty()) {
                    description.append("适应症：").append(aiResponse.getIndications()).append("。");
                }
                if (aiResponse.getUsage() != null && !aiResponse.getUsage().isEmpty()) {
                    description.append("用法用量：").append(aiResponse.getUsage()).append("。");
                }
                if (aiResponse.getPrecautions() != null && !aiResponse.getPrecautions().isEmpty()) {
                    description.append("注意事项：").append(aiResponse.getPrecautions()).append("。");
                }
                if (aiResponse.getAdverseReactions() != null && !aiResponse.getAdverseReactions().isEmpty()) {
                    description.append("不良反应：").append(aiResponse.getAdverseReactions()).append("。");
                }

                // 创建新药品记录
                DrugBase newDrug = DrugBase.builder()
                        .genericName(aiResponse.getGenericName() != null ? aiResponse.getGenericName() : normalizedName)
                        .tradeName(aiResponse.getTradeName())
                        .commonName(aiResponse.getCommonName())
                        .specification(aiResponse.getSpecification() != null ? aiResponse.getSpecification() : nameNormalizer.extractSpecification(rawText))
                        .manufacturer(aiResponse.getManufacturer())
                        .category(aiResponse.getCategory())
                        .description(description.length() > 0 ? description.toString() : null)
                        .build();

                // 保存到数据库
                int result = drugBaseMapper.insert(newDrug);
                if (result > 0) {
                    logger.info("✅ 新药品已成功入库（含AI详情） - drugId: {}, genericName: {}, hasDescription: {}",
                            newDrug.getId(), newDrug.getGenericName(), description.length() > 0);
                    return newDrug;
                } else {
                    logger.error("❌ 新药品入库失败 - normalizedName: {}", normalizedName);
                    return null;
                }
            } else {
                logger.warn("AI未能返回有效药品信息，仅保存基本信息 - drugName: {}", normalizedName);
                
                // 降级：仅保存基本信息
                String specification = nameNormalizer.extractSpecification(rawText);
                String category = deepSeekService.classifyDrugCategory(normalizedName);
                
                DrugBase newDrug = DrugBase.builder()
                        .genericName(normalizedName)
                        .tradeName(normalizedName)
                        .commonName(normalizedName)
                        .specification(specification)
                        .category(category)
                        .build();

                int result = drugBaseMapper.insert(newDrug);
                if (result > 0) {
                    logger.info("✅ 新药品已入库（仅基本信息） - drugId: {}, genericName: {}", newDrug.getId(), newDrug.getGenericName());
                    return newDrug;
                }
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ 调用AI或入库异常 - drugName: {}, error: {}", normalizedName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建识别结果响应
     */
    private OcrResultResponse buildResult(Long drugId, String drugName, BigDecimal score,
                                          String status, String message) {
        OcrResultResponse response = new OcrResultResponse();
        response.setStatus(status);
        
        if (drugId != null) {
            response.setMatchedDrugId(drugId);
            response.setMatchedDrugName(drugName);
            response.setMatchScore(score != null ? score : BigDecimal.ZERO);
        }
        
        return response;
    }

    /**
     * 药品匹配结果内部类
     */
    private static class DrugMatchResult {
        private final boolean matched;
        private final Long drugId;
        private final String drugName;
        private final BigDecimal score;

        public DrugMatchResult(boolean matched, Long drugId, String drugName, BigDecimal score) {
            this.matched = matched;
            this.drugId = drugId;
            this.drugName = drugName;
            this.score = score;
        }

        public boolean isMatched() {
            return matched;
        }

        public Long getDrugId() {
            return drugId;
        }

        public String getDrugName() {
            return drugName;
        }

        public BigDecimal getScore() {
            return score;
        }
    }
}