package com.example.backend.service;

/**
 * 漏服监控服务接口
 */
public interface MissedDoseMonitorService {

    /**
     * 检查所有老人的漏服情况，并通知家属
     */
    void checkMissedDoses();
}
