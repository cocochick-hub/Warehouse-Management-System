package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.entity.AuditLog;
import com.example.wms.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** 分页查询操作日志 */
    @GetMapping("/list")
    public ApiResult<Page<AuditLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<AuditLog> result;

        LocalDateTime start = startTime != null ? startTime : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();

        if (username != null && !username.isEmpty() || action != null && !action.isEmpty()) {
            result = auditLogRepository.findByUsernameContainingAndActionContainingAndCreatedAtBetweenOrderByCreatedAtDesc(
                    username != null ? username : "",
                    action != null ? action : "",
                    start, end, pageable);
        } else {
            result = auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end, pageable);
        }

        return ApiResult.success(result);
    }
}
