package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.DashboardDTO;
import com.example.wms.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/data")
    public ApiResult<DashboardDTO> getDashboardData() {
        DashboardDTO data = dashboardService.getDashboardData();
        return ApiResult.success(data);
    }
}
