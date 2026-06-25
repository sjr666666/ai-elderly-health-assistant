package com.example.backend.service.impl;

import com.example.backend.mapper.DrugRecognitionLogMapper;
import com.example.backend.model.entity.DrugRecognitionLog;
import com.example.backend.service.DrugRecognitionLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 药品识别日志服务实现类
 */
@Service
public class DrugRecognitionLogServiceImpl implements DrugRecognitionLogService {

    private static final Logger logger = LoggerFactory.getLogger(DrugRecognitionLogServiceImpl.class);

    private final DrugRecognitionLogMapper logMapper;

    @Autowired
    public DrugRecognitionLogServiceImpl(DrugRecognitionLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Override
    public void createLog(DrugRecognitionLog log) {
        logMapper.insert(log);
        logger.info("创建药品识别日志 - logId: {}, rawText: {}", log.getId(), log.getRawText());
    }

    @Override
    public void updateMatched(Long logId, Long drugId, String drugName, java.math.BigDecimal matchScore) {
        DrugRecognitionLog log = logMapper.selectById(logId);
        if (log != null) {
            log.setMatched(true);
            log.setMatchedDrugId(drugId);
            log.setMatchedDrugName(drugName);
            log.setMatchScore(matchScore);
            log.setStatus(DrugRecognitionLog.Status.MATCHED.getCode());
            logMapper.updateById(log);
            logger.info("更新药品识别日志为已匹配 - logId: {}, drugId: {}, drugName: {}", logId, drugId, drugName);
        }
    }

    @Override
    public void updateImported(Long logId, Long importedDrugId, String importedDrugName) {
        DrugRecognitionLog log = logMapper.selectById(logId);
        if (log != null) {
            log.setAutoImported(true);
            log.setImportedDrugId(importedDrugId);
            log.setMatchedDrugId(importedDrugId);
            log.setMatchedDrugName(importedDrugName);
            log.setMatched(true);
            log.setMatchScore(new java.math.BigDecimal("1.0"));
            log.setStatus(DrugRecognitionLog.Status.IMPORTED.getCode());
            logMapper.updateById(log);
            logger.info("更新药品识别日志为已入库 - logId: {}, importedDrugId: {}, drugName: {}", logId, importedDrugId, importedDrugName);
        }
    }

    @Override
    public void updateUnmatched(Long logId, String remark) {
        DrugRecognitionLog log = logMapper.selectById(logId);
        if (log != null) {
            log.setMatched(false);
            log.setRemark(remark);
            log.setStatus(DrugRecognitionLog.Status.UNMATCHED.getCode());
            logMapper.updateById(log);
            logger.info("更新药品识别日志为未匹配 - logId: {}, remark: {}", logId, remark);
        }
    }
}