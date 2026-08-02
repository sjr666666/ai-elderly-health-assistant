package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.model.dto.GuardianSummaryDTO;
import com.example.backend.service.GuardianService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 老人端查看已绑定家属
 * elderId 从 SecurityContext 获取，不接受前端传入
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/elder/guardians")
@RequiredArgsConstructor
public class ElderGuardianController {

    private final GuardianService guardianService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未认证");
        }
        return (Long) authentication.getPrincipal();
    }

    @GetMapping
    public ResponseResult<List<GuardianSummaryDTO>> getGuardianList() {
        Long elderId = getCurrentUserId();
        log.info("老人查询已绑定家属列表 - elderId: {}", elderId);
        try {
            return ResponseResult.success(guardianService.getGuardianList(elderId));
        } catch (Exception e) {
            log.error("获取家属列表失败 - elderId: {}", elderId, e);
            return ResponseResult.fail("获取家属列表失败：" + e.getMessage());
        }
    }
}
