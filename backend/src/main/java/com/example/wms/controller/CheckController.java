package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.check.*;
import com.example.wms.service.CheckService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/check")
public class CheckController {

    private final CheckService checkService;

    public CheckController(CheckService checkService) {
        this.checkService = checkService;
    }

    // ==================== PC端 API ====================

    /** 创建盘点任务 */
    @PostMapping("/tasks")
    public ApiResult<CheckTaskDTO> createTask(@RequestBody CreateTaskRequest request, HttpServletRequest httpRequest) {
        // 从 JWT token 中解析用户名（这里从 header 获取，拦截器已验证过 token）
        String username = httpRequest.getHeader("x-username");
        if (username == null || username.isEmpty()) {
            username = "admin";
        }
        return ApiResult.success(checkService.createTask(request, username));
    }

    /** 盘点任务列表 */
    @GetMapping("/tasks")
    public ApiResult<List<CheckTaskDTO>> listTasks() {
        return ApiResult.success(checkService.listTasks());
    }

    /** 任务详情（含明细） */
    @GetMapping("/tasks/{taskId}")
    public ApiResult<List<CheckDetailDTO>> getTaskDetails(@PathVariable Long taskId) {
        return ApiResult.success(checkService.getTaskDetails(taskId));
    }

    /** 完成任务 */
    @PostMapping("/tasks/{taskId}/complete")
    public ApiResult<Void> completeTask(@PathVariable Long taskId) {
        checkService.completeTask(taskId);
        return ApiResult.success(null);
    }

    /** 差异调整 */
    @PostMapping("/details/{detailId}/adjust")
    public ApiResult<Void> adjustDetail(
            @PathVariable Long detailId,
            @RequestParam Integer adjustQty,
            @RequestParam String username) {
        checkService.adjustDetail(detailId, adjustQty, username);
        return ApiResult.success(null);
    }

    // ==================== 移动端 API ====================

    /** 获取进行中的盘点任务列表 */
    @GetMapping("/tasks/active")
    public ApiResult<List<CheckProgressDTO>> listActiveTasks() {
        return ApiResult.success(checkService.listActiveTasks());
    }

    /** 获取盘点明细（扫码前查看） */
    @GetMapping("/details/{detailId}")
    public ApiResult<CheckDetailDTO> getDetail(@PathVariable Long detailId) {
        return ApiResult.success(checkService.getDetail(detailId));
    }

    /** 扫码盘点 */
    @PostMapping("/scan")
    public ApiResult<CheckProgressDTO> scanCheck(@RequestBody ScanRequest request) {
        return ApiResult.success(checkService.scanCheck(request));
    }
}