package com.example.backend.service;

import com.example.backend.model.dto.ElderMedicationPlanDTO;
import com.example.backend.model.dto.ElderSummaryDTO;
import com.example.backend.model.dto.ExpiringDrugDTO;
import com.example.backend.model.dto.GuardianDashboardDTO;
import com.example.backend.model.dto.GuardianRelationRequest;
import com.example.backend.model.dto.GuardianSummaryDTO;
import com.example.backend.model.entity.GuardianElderRelation;

import java.util.List;

/**
 * 监护人服务接口
 */
public interface GuardianService {

    /**
     * 获取监护人仪表盘数据
     *
     * @param guardianId 监护人ID
     * @return 仪表盘数据
     */
    GuardianDashboardDTO getDashboard(Long guardianId);

    /**
     * 获取关联老人列表
     *
     * @param guardianId 监护人ID
     * @return 老人摘要列表
     */
    List<ElderSummaryDTO> getElderList(Long guardianId);

    /**
     * 老人端查询已绑定自己的家属列表
     *
     * @param elderId 老人ID
     * @return 家属摘要列表
     */
    List<GuardianSummaryDTO> getGuardianList(Long elderId);

    /**
     * 获取老人详细信息
     *
     * @param guardianId 监护人ID
     * @param elderId    老人ID
     * @return 老人摘要信息
     */
    ElderSummaryDTO getElderDetail(Long guardianId, Long elderId);

    /**
     * 检查监护人是否有权访问老人数据
     *
     * @param guardianId 监护人ID
     * @param elderId    老人ID
     * @return 是否有权限
     */
    boolean hasPermission(Long guardianId, Long elderId);

    /**
     * 绑定监护关系
     *
     * @param request 绑定请求
     * @return 监护关系
     */
    GuardianElderRelation bindRelation(GuardianRelationRequest request);

    /**
     * 解绑监护关系
     *
     * @param guardianId 监护人ID
     * @param elderId    老人ID
     */
    void unbindRelation(Long guardianId, Long elderId);

    /**
     * 获取老人临期药品列表
     *
     * @param elderId 老人ID
     * @return 临期药品列表
     */
    List<ExpiringDrugDTO> getExpiringDrugs(Long elderId);

    /**
     * 获取老人今日用药计划详情
     *
     * @param elderId 老人ID
     * @return 用药计划详情
     */
    ElderMedicationPlanDTO getMedicationPlan(Long elderId);
}
