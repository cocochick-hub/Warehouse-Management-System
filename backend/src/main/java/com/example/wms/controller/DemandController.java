package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.demand.DemandBatchDTO;
import com.example.wms.dto.demand.DemandCreateRequest;
import com.example.wms.dto.demand.DemandDetailDTO;
import com.example.wms.service.DemandService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 物料需求管理 Controller
 */
@RestController
@RequestMapping("/api/demand")
public class DemandController {

    private final DemandService demandService;

    public DemandController(DemandService demandService) {
        this.demandService = demandService;
    }

    /**
     * 手工创建需求
     */
    @PostMapping("/create")
    public ApiResult<DemandBatchDTO> createDemand(@Valid @RequestBody DemandCreateRequest request,
                                                   @RequestParam(defaultValue = "system") String operator) {
        return ApiResult.success(demandService.createDemand(request, operator));
    }

    /**
     * 分页查询需求明细列表
     */
    @GetMapping("/list")
    public ApiResult<Map<String, Object>> listDemands(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String status) {

        Page<DemandDetailDTO> result = demandService.listDemands(page, size, materialCode, materialName, supplier, status);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", page);
        data.put("size", size);
        return ApiResult.success(data);
    }

    /**
     * 查看批次详情
     */
    @GetMapping("/batch/{batchNo}")
    public ApiResult<DemandBatchDTO> getBatch(@PathVariable String batchNo) {
        return ApiResult.success(demandService.getBatch(batchNo));
    }
}
