package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.model.dto.DrugInfoResponse;
import com.example.backend.model.dto.DrugSearchResponse;
import com.example.backend.service.DrugService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 药品管理控制器
 */
@RestController
@RequestMapping("/api/v1")
public class DrugController {

    private static final Logger logger = LoggerFactory.getLogger(DrugController.class);

    private final DrugService drugService;

    @Autowired
    public DrugController(DrugService drugService) {
        this.drugService = drugService;
    }

    /**
     * 查询药品字典列表
     * GET /api/v1/drug/list?keyword=xxx
     * 
     * @param keyword 搜索关键词（可选，为空则返回所有药品）
     * @return 药品列表
     */
    @GetMapping("/drug/list")
    public ResponseResult<List<DrugInfoResponse>> getDrugList(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        List<DrugInfoResponse> drugList = drugService.getDrugList(keyword);
        return ResponseResult.success("success", drugList);
    }

    /**
     * 根据药品名称查询药品详细信息
     * GET /api/v1/drug/detail?drugName=xxx
     * 
     * @param drugName 药品名称
     * @return 药品详细信息
     */
    @GetMapping("/drug/detail")
    public ResponseResult<DrugDetailResponse> getDrugDetail(
            @RequestParam String drugName) {
        DrugDetailResponse drugDetail = drugService.getDrugDetailByName(drugName);
        if (drugDetail != null) {
            return ResponseResult.success("success", drugDetail);
        } else {
            return ResponseResult.fail("未找到该药品信息");
        }
    }

    /**
     * 智能搜索药品（支持模糊匹配、类别匹配、别名解析）
     * GET /api/v1/drug/search?keyword=xxx
     * 
     * @param keyword 搜索关键词（药品名称、别名、类别关键词等）
     * @return 搜索结果列表（包含匹配度信息）
     */
    @GetMapping("/drug/search")
    public ResponseResult<List<DrugSearchResponse>> searchDrugs(
            @RequestParam String keyword) {
        List<DrugSearchResponse> results = drugService.searchDrugs(keyword);
        return ResponseResult.success("success", results);
    }

    /**
     * 使用AI智能识别药品（支持别名、商品名智能解析）
     * GET /api/v1/drug/ai-search?keyword=xxx
     * 
     * @param keyword 用户输入的关键词
     * @return AI识别后的药品列表
     */
    @GetMapping("/drug/ai-search")
    public ResponseResult<List<DrugSearchResponse>> searchDrugsWithAI(
            @RequestParam String keyword) {
        List<DrugSearchResponse> results = drugService.searchDrugsWithAI(keyword);
        return ResponseResult.success("success", results);
    }
}

