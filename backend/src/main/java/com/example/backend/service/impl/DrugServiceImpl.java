package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.model.dto.DrugInfoResponse;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.service.DrugService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 药品服务实现类
 */
@Service
public class DrugServiceImpl implements DrugService {

    private static final Logger logger = LoggerFactory.getLogger(DrugServiceImpl.class);

    private final DrugBaseMapper drugBaseMapper;

    @Autowired
    public DrugServiceImpl(DrugBaseMapper drugBaseMapper) {
        this.drugBaseMapper = drugBaseMapper;
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
}
