package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.AddMedicineRequest;
import com.example.backend.model.dto.MedicineBoxResponse;
import com.example.backend.model.dto.MedicineShortageWarningDTO;
import com.example.backend.model.dto.UpdateMedicineRequest;
import com.example.backend.service.MedicineBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 药品管理控制器
 * 所有接口通过JWT认证获取当前用户ID，不接受前端传入userId参数
 */
@RestController
@RequestMapping("/api/v1")
public class MedicineController {

    private final MedicineBoxService medicineBoxService;

    @Autowired
    public MedicineController(MedicineBoxService medicineBoxService) {
        this.medicineBoxService = medicineBoxService;
    }

    /**
     * 获取当前认证用户的ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未认证");
        }
        return (Long) authentication.getPrincipal();
    }

    /**
     * 添加药品到药箱
     * POST /api/v1/box
     */
    @PostMapping("/box")
    public ResponseResult<Void> addMedicineToBox(@Valid @RequestBody AddMedicineRequest request) {
        try {
            Long userId = getCurrentUserId();
            medicineBoxService.addMedicineToBox(userId, request);
            return ResponseResult.success("添加成功", null);
        } catch (Exception e) {
            return ResponseResult.fail("添加失败: " + e.getMessage());
        }
    }

    /**
     * 获取药箱列表
     * GET /api/v1/box/list?status=active
     *
     * @param status 状态过滤：active（默认）/ stopped / all
     * @return 药箱列表
     */
    @GetMapping("/box/list")
    public ResponseResult<List<MedicineBoxResponse>> getMedicineBoxList(
            @RequestParam(required = false) String status) {
        try {
            Long userId = getCurrentUserId();
            List<MedicineBoxResponse> boxList = medicineBoxService.getMedicineBoxList(userId, status);
            return ResponseResult.success("success", boxList);
        } catch (Exception e) {
            return ResponseResult.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 搜索药箱中药品
     * GET /api/v1/box/search?keyword=硝苯地平&status=active
     *
     * @param keyword 搜索关键词，支持模糊匹配药品名称、用量或备注
     * @param status 状态过滤：active（默认）/ stopped / all
     * @return 匹配的药箱条目列表
     */
    @GetMapping("/box/search")
    public ResponseResult<List<MedicineBoxResponse>> searchMedicineBox(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        try {
            Long userId = getCurrentUserId();
            List<MedicineBoxResponse> searchResults = medicineBoxService.searchMedicineBox(userId, keyword, status);
            return ResponseResult.success("success", searchResults);
        } catch (Exception e) {
            return ResponseResult.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 修改药箱条目
     * PUT /api/v1/box/{id}
     *
     * @param id      药箱条目ID
     * @param request 更新请求（支持部分字段更新）
     * @return 操作结果
     */
    @PutMapping("/box/{id}")
    public ResponseResult<Void> updateMedicineBoxEntry(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMedicineRequest request) {
        try {
            Long userId = getCurrentUserId();
            medicineBoxService.updateMedicineBoxEntry(userId, id, request);
            return ResponseResult.success("修改成功", null);
        } catch (Exception e) {
            return ResponseResult.fail("修改失败: " + e.getMessage());
        }
    }

    /**
     * 删除药箱条目
     * DELETE /api/v1/box/{id}
     *
     * @param id      药箱条目ID
     * @return 操作结果
     */
    @DeleteMapping("/box/{id}")
    public ResponseResult<Void> deleteMedicineBoxEntry(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            medicineBoxService.deleteMedicineBoxEntry(userId, id);
            return ResponseResult.success("已移除", null);
        } catch (Exception e) {
            return ResponseResult.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新药箱条目状态（部分更新）
     * PATCH /api/v1/box/{id}
     *
     * @param id      药箱条目ID
     * @param request 更新请求（支持部分字段更新）
     * @return 操作结果
     */
    @PatchMapping("/box/{id}")
    public ResponseResult<Void> updateMedicineBoxEntryStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMedicineRequest request) {
        try {
            Long userId = getCurrentUserId();
            medicineBoxService.updateMedicineBoxEntry(userId, id, request);
            return ResponseResult.success("修改成功", null);
        } catch (Exception e) {
            return ResponseResult.fail("修改失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日新过期的药品列表
     * GET /api/v1/box/expired/today
     *
     * @return 今日新过期的药品列表（status=stopped 且 expiryDate <= 今天）
     */
    @GetMapping("/box/expired/today")
    public ResponseResult<List<MedicineBoxResponse>> getTodayExpiredMedicines() {
        try {
            Long userId = getCurrentUserId();
            List<MedicineBoxResponse> expiredList = medicineBoxService.getTodayExpiredMedicines(userId);
            return ResponseResult.success("success", expiredList);
        } catch (Exception e) {
            return ResponseResult.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取缺药预警列表
     * GET /api/v1/box/shortage-warnings
     *
     * 基于服用频率、每次剂量和剩余药量计算剩余天数，
     * 返回剩余天数小于7天的活跃药品预警
     *
     * @return 缺药预警列表，按剩余天数升序排列
     */
    @GetMapping("/box/shortage-warnings")
    public ResponseResult<List<MedicineShortageWarningDTO>> getShortageWarnings() {
        try {
            Long userId = getCurrentUserId();
            List<MedicineShortageWarningDTO> warnings = medicineBoxService.getShortageWarnings(userId);
            return ResponseResult.success("success", warnings);
        } catch (Exception e) {
            return ResponseResult.fail("查询缺药预警失败: " + e.getMessage());
        }
    }
}