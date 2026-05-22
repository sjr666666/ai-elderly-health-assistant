package com.example.backend.service;

import com.example.backend.model.entity.DrugRecognitionLog;

/**
 * 药品识别日志服务接口
 */
public interface DrugRecognitionLogService {

    /**
     * 创建识别日志记录
     *
     * @param log 日志实体
     */
    void createLog(DrugRecognitionLog log);

    /**
     * 更新日志记录（匹配成功）
     *
     * @param logId      日志ID
     * @param drugId     匹配的药品ID
     * @param drugName   匹配的药品名称
     * @param matchScore 匹配分数
     */
    void updateMatched(Long logId, Long drugId, String drugName, java.math.BigDecimal matchScore);

    /**
     * 更新日志记录（自动入库成功）
     *
     * @param logId        日志ID
     * @param importedDrugId 新入库的药品ID
     */
    void updateImported(Long logId, Long importedDrugId);

    /**
     * 更新日志记录（未匹配）
     *
     * @param logId  日志ID
     * @param remark 备注信息
     */
    void updateUnmatched(Long logId, String remark);
}