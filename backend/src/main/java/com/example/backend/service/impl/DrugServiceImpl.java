package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.model.dto.DrugInfoResponse;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.service.DeepSeekService;
import com.example.backend.service.DrugService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Autowired
    public DrugServiceImpl(DrugBaseMapper drugBaseMapper, DeepSeekService deepSeekService) {
        this.drugBaseMapper = drugBaseMapper;
        this.deepSeekService = deepSeekService;
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
}
