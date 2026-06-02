package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.DrugConflictRequest;
import com.example.backend.model.dto.DrugConflictResponse;
import com.example.backend.service.DeepSeekService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 药品冲突检测控制器
 * 提供药品冲突检测相关的REST API接口
 */
@RestController
@RequestMapping("/api/conflict")
@CrossOrigin(origins = "*")
public class DrugConflictController {

    private static final Logger logger = LoggerFactory.getLogger(DrugConflictController.class);

    @Autowired
    private DeepSeekService deepSeekService;

    /**
     * 检测多种药品之间的冲突
     *
     * @param drugNames 药品名称列表
     * @return 冲突检测报告
     */
    @PostMapping("/check")
    public ResponseResult<DrugConflictResponse> checkDrugConflicts(@RequestBody List<String> drugNames) {
        try {
            logger.info("收到药品冲突检测请求 - 药品列表: {}", drugNames);
            
            if (drugNames == null || drugNames.isEmpty()) {
                return ResponseResult.fail("药品列表不能为空");
            }

            DrugConflictResponse result = deepSeekService.checkDrugConflicts(drugNames);
            return ResponseResult.success("冲突检测完成", result);
        } catch (Exception e) {
            logger.error("药品冲突检测失败: ", e);
            return ResponseResult.fail("冲突检测失败: " + e.getMessage());
        }
    }

    /**
     * 全面分析药品冲突（支持药品、保健品、饮料、食物）
     *
     * @param request 冲突检测请求
     * @return 完整的冲突检测报告
     */
    @PostMapping("/analyze")
    public ResponseResult<DrugConflictResponse> analyzeDrugConflicts(@RequestBody DrugConflictRequest request) {
        try {
            logger.info("收到全面药品冲突分析请求 - 药品: {}, 保健品: {}, 饮料: {}, 食物: {}", 
                    request.getDrugNames(), 
                    request.getSupplements(), 
                    request.getBeverages(), 
                    request.getFoods());
            
            if (request.getDrugNames() == null || request.getDrugNames().isEmpty()) {
                return ResponseResult.fail("药品列表不能为空");
            }

            DrugConflictResponse result = deepSeekService.analyzeDrugConflicts(request);
            return ResponseResult.success("冲突分析完成", result);
        } catch (Exception e) {
            logger.error("药品冲突分析失败: ", e);
            return ResponseResult.fail("冲突分析失败: " + e.getMessage());
        }
    }

    /**
     * 快速检测两种药品之间的冲突
     *
     * @param drugA 药品A名称
     * @param drugB 药品B名称
     * @return 冲突检测结果
     */
    @GetMapping("/quick-check")
    public ResponseResult<DrugConflictResponse> quickCheckConflict(
            @RequestParam String drugA,
            @RequestParam String drugB) {
        try {
            logger.info("收到快速冲突检测请求 - 药品A: {}, 药品B: {}", drugA, drugB);
            
            if (drugA == null || drugA.trim().isEmpty() || drugB == null || drugB.trim().isEmpty()) {
                return ResponseResult.fail("药品名称不能为空");
            }

            DrugConflictResponse result = deepSeekService.checkDrugConflicts(List.of(drugA, drugB));
            return ResponseResult.success("快速检测完成", result);
        } catch (Exception e) {
            logger.error("快速冲突检测失败: ", e);
            return ResponseResult.fail("快速检测失败: " + e.getMessage());
        }
    }
}