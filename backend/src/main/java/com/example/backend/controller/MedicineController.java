package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.AddMedicineRequest;
import com.example.backend.model.dto.MedicineBoxResponse;
import com.example.backend.model.dto.MedicineShortageWarningDTO;
import com.example.backend.model.dto.UpdateMedicineRequest;
import com.example.backend.service.MedicineBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 药品管理控制器
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
     * 添加药品到药箱
     * POST /api/v1/box?userId=xxx
     */
    @PostMapping("/box")
    public ResponseResult<Void> addMedicineToBox(
            @RequestParam String userId,
            @Valid @RequestBody AddMedicineRequest request) {
        try {
            // 将 String 转换为 Long
            Long userIdLong = Long.parseLong(userId);
            medicineBoxService.addMedicineToBox(userIdLong, request);
            return ResponseResult.success("添加成功", null);
        } catch (NumberFormatException e) {
            return ResponseResult.fail("无效的用户ID格式");
        } catch (Exception e) {
            return ResponseResult.fail("添加失败: " + e.getMessage());
        }
    }

    /**
     * 获取药箱列表
     * GET /api/v1/box/list?userId=xxx&status=active
     *
     * @param userId 用户ID（雪花算法ID）
     * @param status 状态过滤：active（默认）/ stopped / all
     * @return 药箱列表
     */
    @GetMapping("/box/list")
    public ResponseResult<List<MedicineBoxResponse>> getMedicineBoxList(
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "active") String status) {
        try {
            // 将 String 转换为 Long
            Long userIdLong = Long.parseLong(userId);
            List<MedicineBoxResponse> boxList = medicineBoxService.getMedicineBoxList(userIdLong, status);
            return ResponseResult.success("success", boxList);
        } catch (NumberFormatException e) {
            return ResponseResult.fail("无效的用户ID格式");
        } catch (Exception e) {
            return ResponseResult.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 搜索药箱中药品
     * GET /api/v1/box/search?userId=xxx&keyword=硝苯地平&status=active
     *
     * @param userId 用户ID（雪花算法ID）
     * @param keyword 搜索关键词，支持模糊匹配药品名称、用量或备注
     * @param status 状态过滤：active（默认）/ stopped / all
     * @return 匹配的药箱条目列表
     */
    @GetMapping("/box/search")
    public ResponseResult<List<MedicineBoxResponse>> searchMedicineBox(
            @RequestParam String userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "active") String status) {
        try {
            // 将 String 转换为 Long
            Long userIdLong = Long.parseLong(userId);
            List<MedicineBoxResponse> searchResults = medicineBoxService.searchMedicineBox(userIdLong, keyword, status);
            return ResponseResult.success("success", searchResults);
        } catch (NumberFormatException e) {
            return ResponseResult.fail("无效的用户ID格式");
        } catch (Exception e) {
            return ResponseResult.fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 修改药箱条目
     * PUT /api/v1/box/{id}?userId=xxx
     *
     * @param id      药箱条目ID
     * @param userId  用户ID（雪花算法ID，用于越权校验）
     * @param request 更新请求（支持部分字段更新）
     * @return 操作结果
     */
    @PutMapping("/box/{id}")
    public ResponseResult<Void> updateMedicineBoxEntry(
            @PathVariable Long id,
            @RequestParam String userId,
            @Valid @RequestBody UpdateMedicineRequest request) {
        try {
            // 将 String 转换为 Long
            Long userIdLong = Long.parseLong(userId);
            medicineBoxService.updateMedicineBoxEntry(userIdLong, id, request);
            return ResponseResult.success("修改成功", null);
        } catch (NumberFormatException e) {
            return ResponseResult.fail("无效的用户ID格式");
        } catch (Exception e) {
            return ResponseResult.fail("修改失败: " + e.getMessage());
        }
    }

    /**
     * 删除药箱条目
     * DELETE /api/v1/box/{id}?userId=xxx
     *
     * @param id      药箱条目ID
     * @param userId  用户ID（雪花算法ID，用于越权校验）
     * @return 操作结果
     */
    @DeleteMapping("/box/{id}")
    public ResponseResult<Void> deleteMedicineBoxEntry(
            @PathVariable Long id,
            @RequestParam String userId) {
        try {
            // 将 String 转换为 Long
            Long userIdLong = Long.parseLong(userId);
            medicineBoxService.deleteMedicineBoxEntry(userIdLong, id);
            return ResponseResult.success("已移除", null);
        } catch (NumberFormatException e) {
            return ResponseResult.fail("无效的用户ID格式");
        } catch (Exception e) {
            return ResponseResult.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新药箱条目状态（部分更新）
     * PATCH /api/v1/box/{id}?userId=xxx
     *
     * @param id      药箱条目ID
     * @param userId  用户ID（雪花算法ID，用于越权校验）
     * @param request 更新请求（支持部分字段更新）
     * @return 操作结果
     */
    @PatchMapping("/box/{id}")
    public ResponseResult<Void> updateMedicineBoxEntryStatus(
            @PathVariable Long id,
            @RequestParam String userId,
            @Valid @RequestBody UpdateMedicineRequest request) {
        try {
            // 将 String 转换为 Long
            Long userIdLong = Long.parseLong(userId);
            medicineBoxService.updateMedicineBoxEntry(userIdLong, id, request);
            return ResponseResult.success("修改成功", null);
        } catch (NumberFormatException e) {
            return ResponseResult.fail("无效的用户ID格式");
        } catch (Exception e) {
            return ResponseResult.fail("修改失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日新过期的药品列表
     * GET /api/v1/box/expired/today?userId=xxx
     *
     * @param userId 用户ID（雪花算法ID）
     * @return 今日新过期的药品列表（status=stopped 且 expiryDate <= 今天）
     */
    @GetMapping("/box/expired/today")
    public ResponseResult<List<MedicineBoxResponse>> getTodayExpiredMedicines(
            @RequestParam String userId) {
        try {
            // 将 String 转换为 Long
            Long userIdLong = Long.parseLong(userId);
            List<MedicineBoxResponse> expiredList = medicineBoxService.getTodayExpiredMedicines(userIdLong);
            return ResponseResult.success("success", expiredList);
        } catch (NumberFormatException e) {
            return ResponseResult.fail("无效的用户ID格式");
        } catch (Exception e) {
            return ResponseResult.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取缺药预警列表
     * GET /api/v1/box/shortage-warnings?userId=xxx
     *
     * 基于服用频率、每次剂量和剩余药量计算剩余天数，
     * 返回剩余天数小于7天的活跃药品预警
     *
     * @param userId 用户ID（雪花算法ID）
     * @return 缺药预警列表，按剩余天数升序排列
     */
    @GetMapping("/box/shortage-warnings")
    public ResponseResult<List<MedicineShortageWarningDTO>> getShortageWarnings(
            @RequestParam String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            List<MedicineShortageWarningDTO> warnings = medicineBoxService.getShortageWarnings(userIdLong);
            return ResponseResult.success("success", warnings);
        } catch (NumberFormatException e) {
            return ResponseResult.fail("无效的用户ID格式");
        } catch (Exception e) {
            return ResponseResult.fail("查询缺药预警失败: " + e.getMessage());
        }
    }
}
