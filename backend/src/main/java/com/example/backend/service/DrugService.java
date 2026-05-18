package com.example.backend.service;

import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.model.dto.DrugInfoResponse;

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
}
