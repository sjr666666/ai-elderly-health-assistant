package com.example.backend.service;

import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.model.dto.DrugInfoResponse;
import com.example.backend.model.dto.DrugSearchResponse;

import java.util.List;

/**
 * 药品服务接口
 */
public interface DrugService {

    /**
     * 查询药品字典列表
     *
     * @param keyword 搜索关键词（可选）
     * @return 药品列表
     */
    List<DrugInfoResponse> getDrugList(String keyword);

    /**
     * 根据药品名称查询药品详细信息
     *
     * @param drugName 药品名称
     * @return 药品详细信息
     */
    DrugDetailResponse getDrugDetailByName(String drugName);

    /**
     * 智能搜索药品（支持模糊匹配、类别匹配）
     *
     * @param keyword 搜索关键词
     * @return 搜索结果列表（包含匹配度）
     */
    List<DrugSearchResponse> searchDrugs(String keyword);

    /**
     * 使用AI智能识别药品（支持别名、商品名解析）
     *
     * @param keyword 用户输入的关键词
     * @return AI识别后的药品列表
     */
    List<DrugSearchResponse> searchDrugsWithAI(String keyword);
}
