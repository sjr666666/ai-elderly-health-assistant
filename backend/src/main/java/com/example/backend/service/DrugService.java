package com.example.backend.service;

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
}
