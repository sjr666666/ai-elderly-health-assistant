package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.DrugConflictRequest;
import com.example.backend.model.dto.DrugConflictResponse;
import com.example.backend.model.entity.SysUser;
import com.example.backend.service.DeepSeekService;
import com.example.backend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取当前认证用户的ID（数据库主键）
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未认证");
        }
        return (Long) authentication.getPrincipal();
    }

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

    /**
     * 快速冲突检测（本地规则，不经过AI，适合自动检测场景）
     * 直接返回结果，不等待AI响应，适合新药入箱等自动触发场景
     *
     * @param drugNames 药品名称列表
     * @return 冲突检测报告（本地规则检测结果）
     */
    @PostMapping("/quick-check-local")
    public ResponseResult<DrugConflictResponse> quickCheckLocalConflicts(@RequestBody List<String> drugNames) {
        try {
            logger.info("收到快速本地冲突检测请求 - 药品列表: {}", drugNames);
            
            if (drugNames == null || drugNames.isEmpty()) {
                return ResponseResult.fail("药品列表不能为空");
            }

            // 直接使用本地规则检测，不调用AI
            DrugConflictResponse result = deepSeekService.quickCheckWithLocalRules(drugNames);
            return ResponseResult.success("快速本地检测完成", result);
        } catch (Exception e) {
            logger.error("快速本地冲突检测失败: ", e);
            return ResponseResult.fail("快速本地检测失败: " + e.getMessage());
        }
    }

    /**
     * 结合用户健康档案进行全面冲突检测
     * 根据当前登录用户自动获取身高体重、肾肝功能、孕期/哺乳期、吸烟饮酒等关键用药因素，进行药品冲突检测
     *
     * @param drugNames 药品名称列表
     * @return 冲突检测报告
     */
    @PostMapping("/check-with-profile")
    public ResponseResult<DrugConflictResponse> checkWithUserProfile(
            @RequestBody List<String> drugNames) {
        try {
            Long userId = getCurrentUserId();
            logger.info("收到结合健康档案的冲突检测请求 - 用户ID: {}, 药品列表: {}", userId, drugNames);

            if (drugNames == null || drugNames.isEmpty()) {
                return ResponseResult.fail("药品列表不能为空");
            }

            // userId 为数据库主键 id（由 SecurityContext 提供）
            SysUser user = userMapper.selectById(userId);

            if (user == null) {
                return ResponseResult.fail("用户不存在");
            }

            DrugConflictRequest request = DrugConflictRequest.builder()
                    .drugNames(drugNames)
                    .allergyHistory(user.getAllergyHistory())
                    .chronicDiseases(user.getChronicDiseases())
                    .gender(user.getGender())
                    .age(user.getAge())
                    .height(user.getHeight())
                    .weight(user.getWeight())
                    .kidneyFunction(user.getKidneyFunction())
                    .liverFunction(user.getLiverFunction())
                    .isPregnant(user.getIsPregnant())
                    .isBreastfeeding(user.getIsBreastfeeding())
                    .isSmoking(user.getIsSmoking())
                    .isDrinking(user.getIsDrinking())
                    .detailed(true)
                    .includeAlternatives(true)
                    .build();

            DrugConflictResponse result = deepSeekService.analyzeDrugConflicts(request);
            // 回填 BMI 供前端展示
            result.setBmi(calculateBmi(user.getHeight(), user.getWeight()));
            return ResponseResult.success("冲突检测完成", result);
        } catch (NumberFormatException e) {
            logger.error("用户ID格式错误: ", e);
            return ResponseResult.fail("用户ID格式错误");
        } catch (Exception e) {
            logger.error("结合健康档案的冲突检测失败: ", e);
            return ResponseResult.fail("冲突检测失败: " + e.getMessage());
        }
    }

    /**
     * 计算 BMI（体重 kg / 身高 m²）
     */
    private BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null) {
            return null;
        }
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (heightM.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return weightKg.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
    }
}
