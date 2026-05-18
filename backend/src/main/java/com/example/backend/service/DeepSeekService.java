package com.example.backend.service;

import com.example.backend.model.dto.DrugDetailResponse;

/**
 * DeepSeek AI服务接口
 * 用于调用DeepSeek大语言模型进行药品识别和信息查询
 */
public interface DeepSeekService {

    /**
     * 使用DeepSeek AI分析药品说明书文本，提取药品名称
     *
     * @param ocrText OCR识别的原始文本
     * @return 提取的药品名称，如果未能提取则返回null
     */
    String extractDrugNameWithAI(String ocrText);

    /**
     * 使用DeepSeek AI查询药品详细信息
     *
     * @param drugName 药品名称
     * @return 药品详细信息，如果查询失败则返回null
     */
    DrugDetailResponse queryDrugInfoWithAI(String drugName);
}