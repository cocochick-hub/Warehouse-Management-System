package com.example.wms.controller;

import com.example.wms.entity.InventoryStock;
import com.example.wms.entity.PackageTransfer;
import com.example.wms.entity.InboundKanbanLabel;
import com.example.wms.entity.OutboundHistory;
import com.example.wms.repository.InventoryStockRepository;
import com.example.wms.repository.PackageTransferRepository;
import com.example.wms.repository.InboundKanbanLabelRepository;
import com.example.wms.repository.OutboundHistoryRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 报表导出控制器
 *
 * GET /api/export/inventory  — 库存报表
 * GET /api/export/inbound    — 入库明细
 * GET /api/export/outbound   — 出库明细
 * GET /api/export/transfer   — 转包记录
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final InventoryStockRepository inventoryStockRepository;
    private final InboundKanbanLabelRepository kanbanLabelRepository;
    private final PackageTransferRepository transferRepository;
    private final OutboundHistoryRepository outboundHistoryRepository;

    public ExportController(InventoryStockRepository inventoryStockRepository,
                            InboundKanbanLabelRepository kanbanLabelRepository,
                            PackageTransferRepository transferRepository,
                            OutboundHistoryRepository outboundHistoryRepository) {
        this.inventoryStockRepository = inventoryStockRepository;
        this.kanbanLabelRepository = kanbanLabelRepository;
        this.transferRepository = transferRepository;
        this.outboundHistoryRepository = outboundHistoryRepository;
    }

    /** 导出库存报表 */
    @GetMapping("/inventory")
    public void exportInventory(HttpServletResponse response) throws IOException {
        List<InventoryStock> list = inventoryStockRepository.findAll();

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("库存报表");
        Row header = sheet.createRow(0);
        String[] titles = {"物料编码", "物料名称", "供应商", "库区", "库存数量", "入库单号", "入库时间", "封存状态"};
        for (int i = 0; i < titles.length; i++) {
            header.createCell(i).setCellValue(titles[i]);
        }

        int rowIdx = 1;
        for (InventoryStock s : list) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(s.getMaterialCode());
            row.createCell(1).setCellValue(s.getMaterialName());
            row.createCell(2).setCellValue(s.getSupplier());
            row.createCell(3).setCellValue(s.getWarehouseArea());
            row.createCell(4).setCellValue(s.getOnHandQty() != null ? s.getOnHandQty() : 0);
            row.createCell(5).setCellValue(s.getLastInboundDocNo() != null ? s.getLastInboundDocNo() : "-");
            row.createCell(6).setCellValue(s.getLastInboundAt() != null ? s.getLastInboundAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-");
            row.createCell(7).setCellValue(s.getTransferStatus() != null ? s.getTransferStatus() : "-");
        }

        for (int i = 0; i < titles.length; i++) sheet.autoSizeColumn(i);
        writeResponse(response, wb, "库存报表");
    }

    /** 导出入库看板明细 */
    @GetMapping("/inbound")
    public void exportInbound(HttpServletResponse response) throws IOException {
        List<InboundKanbanLabel> list = kanbanLabelRepository.findAll();

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("入库明细");
        Row header = sheet.createRow(0);
        String[] titles = {"看板号", "入库单号", "物料编码", "物料名称", "供应商", "数量", "库区", "状态", "封存", "转包状态", "创建时间"};
        for (int i = 0; i < titles.length; i++) {
            header.createCell(i).setCellValue(titles[i]);
        }

        int rowIdx = 1;
        for (InboundKanbanLabel k : list) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(k.getKanbanNo());
            row.createCell(1).setCellValue(k.getDocNo());
            row.createCell(2).setCellValue(k.getMaterialCode());
            row.createCell(3).setCellValue(k.getMaterialName());
            row.createCell(4).setCellValue(k.getSupplierName());
            row.createCell(5).setCellValue(k.getLabelQty());
            row.createCell(6).setCellValue(k.getWarehouseArea());
            row.createCell(7).setCellValue(k.getLabelStatus());
            row.createCell(8).setCellValue(Boolean.TRUE.equals(k.getSealed()) ? "已封存" : "正常");
            row.createCell(9).setCellValue(k.getTransferStatus());
            row.createCell(10).setCellValue(k.getCreatedAt() != null ? k.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-");
        }

        for (int i = 0; i < titles.length; i++) sheet.autoSizeColumn(i);
        writeResponse(response, wb, "入库明细");
    }

    /** 导出出库明细 */
    @GetMapping("/outbound")
    public void exportOutbound(HttpServletResponse response) throws IOException {
        List<OutboundHistory> list = outboundHistoryRepository.findAll();

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("出库明细");
        Row header = sheet.createRow(0);
        String[] titles = {"出库单号", "物料编码", "物料名称", "供应商", "出库数量", "源入库单号", "库区", "操作人", "状态", "出库时间"};
        for (int i = 0; i < titles.length; i++) {
            header.createCell(i).setCellValue(titles[i]);
        }

        int rowIdx = 1;
        for (OutboundHistory h : list) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(h.getDocNo());
            row.createCell(1).setCellValue(h.getMaterialCode());
            row.createCell(2).setCellValue(h.getMaterialName());
            row.createCell(3).setCellValue(h.getSupplierName());
            row.createCell(4).setCellValue(h.getIssueQty() != null ? h.getIssueQty() : 0);
            row.createCell(5).setCellValue(h.getSourceInboundDoc() != null ? h.getSourceInboundDoc() : "-");
            row.createCell(6).setCellValue(h.getWarehouseArea() != null ? h.getWarehouseArea() : "默认库区");
            row.createCell(7).setCellValue(h.getIssuedBy() != null ? h.getIssuedBy() : "-");
            row.createCell(8).setCellValue(h.getStatus() != null ? h.getStatus() : "-");
            row.createCell(9).setCellValue(h.getCreatedAt() != null ? h.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-");
        }

        for (int i = 0; i < titles.length; i++) sheet.autoSizeColumn(i);
        writeResponse(response, wb, "出库明细");
    }

    /** 导出转包记录 */
    @GetMapping("/transfer")
    public void exportTransfer(HttpServletResponse response) throws IOException {
        List<PackageTransfer> list = transferRepository.findAll();

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("转包记录");
        Row header = sheet.createRow(0);
        String[] titles = {"源看板号", "目标看板号", "转移数量", "转移前数量", "转移后数量", "物料编码", "物料名称", "供应商", "操作人", "操作时间"};
        for (int i = 0; i < titles.length; i++) {
            header.createCell(i).setCellValue(titles[i]);
        }

        int rowIdx = 1;
        for (PackageTransfer t : list) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(t.getSourceKanbanNo());
            row.createCell(1).setCellValue(t.getTargetKanbanNo());
            row.createCell(2).setCellValue(t.getTransferQty());
            row.createCell(3).setCellValue(t.getSourceQtyBefore());
            row.createCell(4).setCellValue(t.getSourceQtyAfter());
            row.createCell(5).setCellValue(t.getMaterialCode());
            row.createCell(6).setCellValue(t.getMaterialName());
            row.createCell(7).setCellValue(t.getSupplierName() != null ? t.getSupplierName() : "-");
            row.createCell(8).setCellValue(t.getOperator() != null ? t.getOperator() : "-");
            row.createCell(9).setCellValue(t.getCreatedAt() != null ? t.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-");
        }

        for (int i = 0; i < titles.length; i++) sheet.autoSizeColumn(i);
        writeResponse(response, wb, "转包记录");
    }

    private void writeResponse(HttpServletResponse response, Workbook wb, String name) throws IOException {
        String filename = URLEncoder.encode(name + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        wb.write(response.getOutputStream());
        wb.close();
    }
}
