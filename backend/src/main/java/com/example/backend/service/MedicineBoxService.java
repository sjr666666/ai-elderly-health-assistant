package com.example.backend.service;

import com.example.backend.model.dto.AddMedicineRequest;
import com.example.backend.model.dto.MedicineBoxResponse;
import com.example.backend.model.dto.UpdateMedicineRequest;

import java.util.List;

/**
 * 药箱服务接口
 */
public interface MedicineBoxService {

    /**
     * 添加药品到药箱
     *
     * @param userId  用户ID
     * @param request 添加药品请求
     */
    void addMedicineToBox(Long userId, AddMedicineRequest request);

    /**
     * 获取药箱列表
     *
     * @param userId 用户ID
     * @param status 状态过滤（active/stopped/all），默认为 active
     * @return 药箱列表
     */
    List<MedicineBoxResponse> getMedicineBoxList(Long userId, String status);

    /**
     * 搜索药箱中药品
     *
     * @param userId 用户ID
     * @param keyword 搜索关键词，支持模糊匹配药品名称、用量或备注
     * @param status 状态过滤（active/stopped/all），默认为 active
     * @return 匹配的药箱条目列表
     */
    List<MedicineBoxResponse> searchMedicineBox(Long userId, String keyword, String status);

    /**
     * 修改药箱条目
     *
     * @param userId 用户ID
     * @param boxId  药箱条目ID
     * @param request 更新请求（支持部分字段更新）
     */
    void updateMedicineBoxEntry(Long userId, Long boxId, UpdateMedicineRequest request);

    /**
     * 删除药箱条目（逻辑删除，将状态改为 stopped）
     *
     * @param userId 用户ID
     * @param boxId  药箱条目ID
     */
    void deleteMedicineBoxEntry(Long userId, Long boxId);

    /**
     * 获取今日新过期的药品列表
     *
     * @param userId 用户ID
     * @return 今日新过期的药品列表（status=stopped 且 expiryDate <= 今天）
     */
    List<MedicineBoxResponse> getTodayExpiredMedicines(Long userId);
}
