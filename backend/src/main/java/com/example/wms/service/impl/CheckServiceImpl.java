package com.example.wms.service.impl;

import com.example.wms.dto.check.*;
import com.example.wms.entity.InventoryCheckDetail;
import com.example.wms.entity.InventoryCheckTask;
import com.example.wms.entity.InventoryStock;
import com.example.wms.entity.OutboundHistory;
import com.example.wms.repository.*;
import com.example.wms.service.CheckService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CheckServiceImpl implements CheckService {

    private final InventoryCheckTaskRepo taskRepo;
    private final InventoryCheckDetailRepo detailRepo;
    private final InventoryStockRepository stockRepo;
    private final OutboundHistoryRepository outboundHistoryRepo;

    public CheckServiceImpl(InventoryCheckTaskRepo taskRepo,
                            InventoryCheckDetailRepo detailRepo,
                            InventoryStockRepository stockRepo,
                            OutboundHistoryRepository outboundHistoryRepo) {
        this.taskRepo = taskRepo;
        this.detailRepo = detailRepo;
        this.stockRepo = stockRepo;
        this.outboundHistoryRepo = outboundHistoryRepo;
    }

    @Override
    @Transactional
    public CheckTaskDTO createTask(CreateTaskRequest request, String username) {
        // 1. 生成盘点单号 PD-yyyyMMdd-序号
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PD-" + today + "-";
        int seq = 1;
        var latest = taskRepo.findTopByTaskNoStartingWithOrderByTaskNoDesc(prefix);
        if (latest.isPresent()) {
            String lastNo = latest.get().getTaskNo();
            String seqStr = lastNo.substring(lastNo.lastIndexOf('-') + 1);
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException ignored) {}
        }
        String taskNo = prefix + String.format("%03d", seq);

        // 2. 创建任务
        InventoryCheckTask task = new InventoryCheckTask();
        task.setTaskNo(taskNo);
        task.setTaskName(request.getTaskName());
        task.setCheckType(request.getCheckType() != null ? request.getCheckType() : "明盘");
        task.setStatus("进行中");
        task.setWarehouseArea(request.getWarehouseArea());
        task.setMaterialCode(request.getMaterialCode());
        task.setCreatedBy(username);
        task = taskRepo.save(task);

        // 3. 查询匹配的库存记录，创建明细
        List<InventoryStock> stocks = queryMatchingStocks(request);
        List<InventoryCheckDetail> details = new ArrayList<>();
        for (InventoryStock stock : stocks) {
            InventoryCheckDetail d = new InventoryCheckDetail();
            d.setTaskId(task.getId());
            d.setTaskNo(taskNo);
            d.setMaterialCode(stock.getMaterialCode());
            d.setMaterialName(stock.getMaterialName());
            d.setSupplier(stock.getSupplier());
            d.setWarehouseArea(stock.getWarehouseArea());
            d.setSystemQty(safeInt(stock.getOnHandQty()));
            d.setStatus("待盘");
            details.add(d);
        }
        detailRepo.saveAll(details);

        // 4. 返回DTO
        return toTaskDTO(task, details.size(), 0);
    }

    private List<InventoryStock> queryMatchingStocks(CreateTaskRequest request) {
        String area = trimToNull(request.getWarehouseArea());
        String matCode = trimToNull(request.getMaterialCode());
        if (area != null && matCode != null) {
            return stockRepo.findAll().stream()
                    .filter(s -> equals(area, s.getWarehouseArea()) && like(matCode, s.getMaterialCode()))
                    .collect(Collectors.toList());
        } else if (area != null) {
            return stockRepo.findAll().stream()
                    .filter(s -> equals(area, s.getWarehouseArea()))
                    .collect(Collectors.toList());
        } else if (matCode != null) {
            return stockRepo.findAll().stream()
                    .filter(s -> like(matCode, s.getMaterialCode()))
                    .collect(Collectors.toList());
        } else {
            return stockRepo.findAll();
        }
    }

    @Override
    public List<CheckTaskDTO> listTasks() {
        List<InventoryCheckTask> tasks = taskRepo.findAllByOrderByCreatedAtDesc();
        return tasks.stream().map(task -> {
            int total = detailRepo.countByTaskId(task.getId());
            int checked = detailRepo.countByTaskIdAndStatus(task.getId(), "已盘");
            return toTaskDTO(task, total, checked);
        }).collect(Collectors.toList());
    }

    @Override
    public List<CheckDetailDTO> getTaskDetails(Long taskId) {
        return detailRepo.findByTaskId(taskId).stream()
                .map(this::toDetailDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void completeTask(Long taskId) {
        InventoryCheckTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        if (!"进行中".equals(task.getStatus())) {
            throw new IllegalStateException("只能完成进行中的任务");
        }
        task.setStatus("已完成");
        task.setCompletedAt(LocalDateTime.now());
        taskRepo.save(task);
    }

    @Override
    @Transactional
    public void adjustDetail(Long detailId, Integer adjustQty, String username) {
        InventoryCheckDetail detail = detailRepo.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("明细不存在"));
        if (!"已盘".equals(detail.getStatus())) {
            throw new IllegalStateException("只能调整已盘的明细");
        }
        if (detail.getDiffQty() == null || detail.getDiffQty() == 0) {
            throw new IllegalStateException("无差异，无需调整");
        }

        // 更新库存
        String matCode = detail.getMaterialCode();
        String supplier = detail.getSupplier();
        String warehouseArea = detail.getWarehouseArea();
        InventoryStock stock = stockRepo.findByMaterialCodeAndSupplierAndWarehouseArea(matCode, supplier, warehouseArea)
                .orElseThrow(() -> new IllegalStateException("库存记录不存在"));

        int diff = detail.getDiffQty(); // actual - system
        int newQty = adjustQty; // 调整后的目标数量

        // 更新库存
        stock.setOnHandQty(newQty);
        stock.setUpdatedBy(username);
        stockRepo.save(stock);

        // 盘盈：diff > 0，需要记录入库历史（虚拟入库单）
        if (diff > 0) {
            OutboundHistory inbound = new OutboundHistory();
            inbound.setOutboundOrderId(null);
            inbound.setOutboundDetailId(null);
            inbound.setDocNo("PD-" + detail.getTaskNo() + "-盈");
            inbound.setMaterialCode(matCode);
            inbound.setMaterialName(detail.getMaterialName());
            inbound.setSupplierName(supplier);
            inbound.setIssueQty(diff); // 盘盈入库量
            inbound.setWarehouseArea(warehouseArea);
            inbound.setIssuedBy(username);
            inbound.setStatus("已入库"); // 盘盈是虚拟入库
            inbound.setCreatedAt(LocalDateTime.now());
            outboundHistoryRepo.save(inbound);
        }
        // 盘亏：diff < 0，需要记录出库历史
        if (diff < 0) {
            OutboundHistory outbound = new OutboundHistory();
            outbound.setOutboundOrderId(null);
            outbound.setOutboundDetailId(null);
            outbound.setDocNo("PD-" + detail.getTaskNo() + "-亏");
            outbound.setMaterialCode(matCode);
            outbound.setMaterialName(detail.getMaterialName());
            outbound.setSupplierName(supplier);
            outbound.setIssueQty(Math.abs(diff)); // 盘亏出库量
            outbound.setWarehouseArea(warehouseArea);
            outbound.setIssuedBy(username);
            outbound.setStatus("已出库"); // 盘亏是虚拟出库
            outbound.setCreatedAt(LocalDateTime.now());
            outboundHistoryRepo.save(outbound);
        }

        // 标记已调整
        detail.setStatus("已调整");
        detailRepo.save(detail);
    }

    @Override
    public List<CheckProgressDTO> listActiveTasks() {
        return taskRepo.findByStatusOrderByCreatedAtDesc("进行中").stream()
                .map(task -> {
                    int total = detailRepo.countByTaskId(task.getId());
                    int checked = detailRepo.countByTaskIdAndStatus(task.getId(), "已盘");
                    int diff = detailRepo.findByTaskId(task.getId()).stream()
                            .filter(d -> "已盘".equals(d.getStatus()) && d.getDiffQty() != null && d.getDiffQty() != 0)
                            .mapToInt(d -> 1)
                            .sum();
                    int progress = total > 0 ? (checked * 100 / total) : 0;
                    return new CheckProgressDTO(task.getId(), task.getTaskNo(), task.getTaskName(),
                            task.getCheckType(), total, checked, progress, diff);
                })
                .collect(Collectors.toList());
    }

    @Override
    public CheckDetailDTO getDetail(Long detailId) {
        InventoryCheckDetail d = detailRepo.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("明细不存在"));
        return toDetailDTO(d);
    }

    @Override
    @Transactional
    public CheckProgressDTO scanCheck(ScanRequest request) {
        // 1. 查找明细
        InventoryCheckDetail detail;
        if (request.getWarehouseArea() != null && !request.getWarehouseArea().isEmpty()) {
            detail = detailRepo.findByTaskIdAndMaterialCodeAndWarehouseArea(
                    request.getTaskId(), request.getMaterialCode(), request.getWarehouseArea())
                    .orElseThrow(() -> new IllegalArgumentException("未找到盘点明细"));
        } else {
            detail = detailRepo.findByTaskIdAndMaterialCode(request.getTaskId(), request.getMaterialCode())
                    .orElseThrow(() -> new IllegalArgumentException("未找到盘点明细"));
        }

        if (!"待盘".equals(detail.getStatus())) {
            throw new IllegalStateException("该物料已盘点，请勿重复扫码");
        }

        // 2. 更新实际数量和差异
        detail.setActualQty(request.getActualQty());
        detail.setDiffQty(request.getActualQty() - detail.getSystemQty());
        detail.setStatus("已盘");
        detail.setCheckedBy(request.getCheckedBy());
        detail.setCheckedAt(LocalDateTime.now());
        detailRepo.save(detail);

        // 3. 返回进度
        InventoryCheckTask task = taskRepo.findById(request.getTaskId())
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        int total = detailRepo.countByTaskId(task.getId());
        int checked = detailRepo.countByTaskIdAndStatus(task.getId(), "已盘");
        int diff = detailRepo.findByTaskId(task.getId()).stream()
                .filter(d -> "已盘".equals(d.getStatus()) && d.getDiffQty() != null && d.getDiffQty() != 0)
                .mapToInt(d -> 1)
                .sum();
        int progress = total > 0 ? (checked * 100 / total) : 0;
        return new CheckProgressDTO(task.getId(), task.getTaskNo(), task.getTaskName(),
                task.getCheckType(), total, checked, progress, diff);
    }

    private CheckTaskDTO toTaskDTO(InventoryCheckTask task, int total, int checked) {
        int progress = total > 0 ? (checked * 100 / total) : 0;
        CheckTaskDTO dto = new CheckTaskDTO();
        dto.setId(task.getId());
        dto.setTaskNo(task.getTaskNo());
        dto.setTaskName(task.getTaskName());
        dto.setCheckType(task.getCheckType());
        dto.setStatus(task.getStatus());
        dto.setWarehouseArea(task.getWarehouseArea());
        dto.setMaterialCode(task.getMaterialCode());
        dto.setCreatedBy(task.getCreatedBy());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        dto.setDetailCount(total);
        dto.setCheckedCount(checked);
        dto.setProgressPercent(progress);
        return dto;
    }

    private CheckDetailDTO toDetailDTO(InventoryCheckDetail d) {
        return new CheckDetailDTO(
                d.getId(), d.getTaskId(), d.getTaskNo(),
                d.getMaterialCode(), d.getMaterialName(), d.getSupplier(),
                d.getWarehouseArea(), d.getSystemQty(), d.getActualQty(),
                d.getDiffQty(), d.getStatus(), d.getCheckedBy(),
                d.getCheckedAt(), d.getCreatedAt()
        );
    }

    private int safeInt(Integer v) { return v == null ? 0 : v; }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean equals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.trim().equals(b.trim());
    }

    private boolean like(String pattern, String value) {
        if (pattern == null || value == null) return false;
        return value.contains(pattern);
    }
}