package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.DrugInfoResponse;
import com.example.backend.service.DrugService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 药品管理控制器
 */
@RestController
@RequestMapping("/api/v1")
public class DrugController {

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
        try {
            List<DrugInfoResponse> drugList = drugService.getDrugList(keyword);
            return ResponseResult.success("success", drugList);
        } catch (Exception e) {
            return ResponseResult.fail("查询失败: " + e.getMessage());
        }
    }
}
