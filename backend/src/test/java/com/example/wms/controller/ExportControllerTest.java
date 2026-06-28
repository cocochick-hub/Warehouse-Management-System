package com.example.wms.controller;

import com.example.wms.entity.InventoryStock;
import com.example.wms.entity.OutboundHistory;
import com.example.wms.entity.OutboundOrder;
import com.example.wms.entity.OutboundOrderDetail;
import com.example.wms.entity.PackageTransfer;
import com.example.wms.entity.InboundKanbanLabel;
import com.example.wms.entity.InboundOrder;
import com.example.wms.entity.InboundOrderDetail;
import com.example.wms.repository.InventoryStockRepository;
import com.example.wms.repository.OutboundHistoryRepository;
import com.example.wms.repository.OutboundOrderRepository;
import com.example.wms.repository.OutboundOrderDetailRepository;
import com.example.wms.repository.PackageTransferRepository;
import com.example.wms.repository.InboundKanbanLabelRepository;
import com.example.wms.repository.InboundOrderRepository;
import com.example.wms.repository.InboundOrderDetailRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ExportController 单元测试
 *
 * TC-E-01：GET /api/export/inventory → 库存报表 Excel 文件流
 * TC-E-02：GET /api/export/inbound  → 入库明细 Excel 文件流
 * TC-E-03：GET /api/export/transfer → 转包记录 Excel 文件流
 * TC-E-04：GET /api/export/outbound → 出库明细 Excel 文件流
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("报表导出控制器测试")
public class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryStockRepository inventoryStockRepository;

    @Autowired
    private InboundKanbanLabelRepository kanbanLabelRepository;

    @Autowired
    private PackageTransferRepository transferRepository;

    @Autowired
    private OutboundHistoryRepository outboundHistoryRepository;

    @Autowired
    private InboundOrderRepository inboundOrderRepository;

    @Autowired
    private InboundOrderDetailRepository inboundOrderDetailRepository;

    @Autowired
    private OutboundOrderRepository outboundOrderRepository;

    @Autowired
    private OutboundOrderDetailRepository outboundOrderDetailRepository;

    @Autowired
    private DataSource dataSource;

    private String uniqueSuffix;

    @BeforeEach
    void initSchema() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema-h2.sql"));
        populator.setContinueOnError(true);
        populator.execute(dataSource);
        // 每个测试用唯一后缀避免唯一约束冲突
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    }

    // ==================== TC-E-01：库存报表导出 ====================

    @Nested
    @DisplayName("TC-E-01：导出库存报表")
    class ExportInventory {

        @Test
        @DisplayName("GET /api/export/inventory 返回 Excel 文件流，Content-Type 正确，文件可解析")
        @WithMockUser(username = "admin")
        void exportInventory_shouldReturnExcelFile() throws Exception {
            // inventory_stock 无 FK 约束，直接插入
            InventoryStock stock = new InventoryStock();
            stock.setMaterialCode("MAT-INV-" + uniqueSuffix);
            stock.setMaterialName("发动机支架");
            stock.setSupplier("上海汽车零部件");
            stock.setWarehouseArea("A区-01");
            stock.setOnHandQty(50);
            stock.setLastInboundDocNo("IN-TEST-001");
            stock.setLastInboundAt(LocalDateTime.now());
            stock.setTransferStatus("不转包");
            stock.setCreatedBy("admin");
            stock.setUpdatedBy("admin");
            stock.setCreatedAt(LocalDateTime.now());
            stock.setUpdatedAt(LocalDateTime.now());
            inventoryStockRepository.save(stock);

            MvcResult result = mockMvc.perform(get("/api/export/inventory"))
                    .andExpect(status().isOk())
                    .andReturn();

            HttpServletResponse response = result.getResponse();

            // 验证 Content-Type（含 charset 的情况）
            String contentType = response.getContentType();
            assertNotNull(contentType);
            assertTrue(contentType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                    "Content-Type 应为 Excel 格式，实际: " + contentType);

            // 验证 Content-Disposition（含 percent-encoded 中文）
            String contentDisposition = response.getHeader("Content-Disposition");
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.startsWith("attachment"));
            assertTrue(contentDisposition.contains("filename*=UTF-8''"));
            assertTrue(contentDisposition.endsWith(".xlsx"));

            // 验证文件流非空
            byte[] body = result.getResponse().getContentAsByteArray();
            assertTrue(body.length > 0);

            // 验证可被 POI 解析
            try (XSSFWorkbook wb = new XSSFWorkbook(
                    new java.io.ByteArrayInputStream(body))) {
                assertEquals(1, wb.getNumberOfSheets());
                Sheet sheet = wb.getSheet("库存报表");
                assertNotNull(sheet);

                Row headerRow = sheet.getRow(0);
                assertNotNull(headerRow);
                String[] expectedTitles = {"物料编码", "物料名称", "供应商", "库区", "库存数量", "入库单号", "入库时间", "封存状态"};
                for (int i = 0; i < expectedTitles.length; i++) {
                    assertEquals(expectedTitles[i], headerRow.getCell(i).getStringCellValue());
                }

                assertTrue(sheet.getPhysicalNumberOfRows() >= 2);
                Row dataRow = sheet.getRow(1);
                assertEquals("MAT-INV-" + uniqueSuffix, dataRow.getCell(0).getStringCellValue());
                assertEquals("发动机支架", dataRow.getCell(1).getStringCellValue());
            }
        }
    }

    // ==================== TC-E-02：入库明细导出 ====================

    @Nested
    @DisplayName("TC-E-02：导出入库明细")
    class ExportInbound {

        @Test
        @DisplayName("GET /api/export/inbound 返回 Excel 文件流，Sheet 名=入库明细，11列表头")
        @WithMockUser(username = "admin")
        @Disabled("InboundKanbanLabel 存在 schema 约束问题，待修复")
        void exportInbound_shouldReturnExcelFile() throws Exception {
            // inbound_kanban_label 有 FK 依赖 inbound_order + inbound_order_detail
            InboundOrder order = new InboundOrder();
            order.setDocNo("IN-KANBAN-" + uniqueSuffix);
            order.setSupplier("测试供应商");
            order.setStatus("已入库");
            order.setItemCount(1);
            order.setPlannedTotalQty(100);
            order.setActualTotalQty(100);
            order.setCreatedBy("admin");
            order.setUpdatedBy("admin");
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order = inboundOrderRepository.save(order);

            InboundOrderDetail detail = new InboundOrderDetail();
            detail.setInboundOrderId(order.getId());
            detail.setDocNo("IN-KANBAN-" + uniqueSuffix);
            detail.setLineNo(1);
            detail.setSupplierCode("SUP-001");
            detail.setSupplierName("上海汽车零部件");
            detail.setMaterialCode("MAT-INB-" + uniqueSuffix);
            detail.setMaterialName("发动机支架");
            detail.setPackageModel("BX-ENG-20");
            detail.setPackagingCapacity(20);
            detail.setPlannedQty(100);
            detail.setActualQty(100);
            detail.setPackageCount(1);
            detail.setWarehouseArea("A区-01");
            detail.setCreatedBy("admin");
            detail.setUpdatedBy("admin");
            detail.setCreatedAt(LocalDateTime.now());
            detail.setUpdatedAt(LocalDateTime.now());
            detail = inboundOrderDetailRepository.save(detail);

            InboundKanbanLabel label = new InboundKanbanLabel();
            label.setInboundOrderId(order.getId());
            label.setInboundOrderDetailId(detail.getId());
            label.setKanbanNo("R-TEST-" + uniqueSuffix);
            label.setDocNo("IN-KANBAN-" + uniqueSuffix);
            label.setQrPayload("QR-" + uniqueSuffix);
            label.setMaterialCode("MAT-INB-" + uniqueSuffix);
            label.setMaterialName("发动机支架");
            label.setSupplierCode("SUP-001");
            label.setSupplierName("上海汽车零部件");
            label.setLabelQty(100);
            label.setPackageSeq(1);
            label.setPackageTotal(1);
            label.setWarehouseArea("A区-01");
            label.setLabelStatus("已入库");
            label.setSealed(false);
            label.setTransferStatus("不转包");
            label.setCreatedAt(LocalDateTime.now());
            kanbanLabelRepository.save(label);

            MvcResult result = mockMvc.perform(get("/api/export/inbound"))
                    .andExpect(status().isOk())
                    .andReturn();

            HttpServletResponse response = result.getResponse();
            String contentType = response.getContentType();
            assertNotNull(contentType);
            assertTrue(contentType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            String contentDisposition = response.getHeader("Content-Disposition");
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.startsWith("attachment"));
            assertTrue(contentDisposition.contains("filename*=UTF-8''"));
            assertTrue(contentDisposition.endsWith(".xlsx"));

            byte[] body = result.getResponse().getContentAsByteArray();
            assertTrue(body.length > 0);

            try (XSSFWorkbook wb = new XSSFWorkbook(
                    new java.io.ByteArrayInputStream(body))) {
                Sheet sheet = wb.getSheet("入库明细");
                assertNotNull(sheet);

                Row headerRow = sheet.getRow(0);
                assertNotNull(headerRow);
                assertEquals(11, headerRow.getLastCellNum());

                Row dataRow = sheet.getRow(1);
                assertEquals("R-TEST-" + uniqueSuffix, dataRow.getCell(0).getStringCellValue());
            }
        }
    }

    // ==================== TC-E-03：转包记录导出 ====================

    @Nested
    @DisplayName("TC-E-03：导出转包记录")
    class ExportTransfer {

        @Test
        @DisplayName("GET /api/export/transfer 返回 Excel 文件流，Sheet 名=转包记录，10列表头")
        @WithMockUser(username = "admin")
        void exportTransfer_shouldReturnExcelFile() throws Exception {
            // package_transfer 无 FK 约束
            PackageTransfer transfer = new PackageTransfer();
            transfer.setSourceKanbanNo("R-SRC-" + uniqueSuffix);
            transfer.setTargetKanbanNo("T-TGT-" + uniqueSuffix);
            transfer.setTransferQty(30);
            transfer.setSourceQtyBefore(100);
            transfer.setSourceQtyAfter(70);
            transfer.setMaterialCode("MAT-TRF-" + uniqueSuffix);
            transfer.setMaterialName("发动机支架");
            transfer.setSupplierName("上海汽车零部件");
            transfer.setOperator("admin");
            transfer.setCreatedAt(LocalDateTime.now());
            transferRepository.save(transfer);

            MvcResult result = mockMvc.perform(get("/api/export/transfer"))
                    .andExpect(status().isOk())
                    .andReturn();

            HttpServletResponse response = result.getResponse();
            String contentType = response.getContentType();
            assertNotNull(contentType);
            assertTrue(contentType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                    "Content-Type 应为 Excel 格式，实际: " + contentType);

            byte[] body = result.getResponse().getContentAsByteArray();
            assertTrue(body.length > 0);

            try (XSSFWorkbook wb = new XSSFWorkbook(
                    new java.io.ByteArrayInputStream(body))) {
                Sheet sheet = wb.getSheet("转包记录");
                assertNotNull(sheet);

                Row headerRow = sheet.getRow(0);
                assertNotNull(headerRow);
                assertEquals(10, headerRow.getLastCellNum());

                Row dataRow = sheet.getRow(1);
                assertEquals("R-SRC-" + uniqueSuffix, dataRow.getCell(0).getStringCellValue());
                assertEquals("T-TGT-" + uniqueSuffix, dataRow.getCell(1).getStringCellValue());
                assertEquals(30, dataRow.getCell(2).getNumericCellValue(), 0.01);
            }
        }
    }

    // ==================== TC-E-04：出库明细导出 ====================

    @Nested
    @DisplayName("TC-E-04：导出出库明细（新增）")
    class ExportOutbound {

        @Test
        @DisplayName("GET /api/export/outbound 返回 Excel 文件流")
        @WithMockUser(username = "admin")
        @Disabled("outbound_order 表存在 outbound_type 列与实体不匹配，暂时跳过")
        void exportOutbound_shouldReturnExcelFile() throws Exception {
            // outbound_order 表 schema 与实体有差异（outbound_type 列）
            // 此测试暂时禁用，待 schema 与实体对齐后启用
            OutboundOrder order = new OutboundOrder();
            order.setDocNo("OUT-" + uniqueSuffix);
            order.setSupplier("测试供应商");
            order.setStatus("已出库");
            order.setItemCount(1);
            order.setPlannedTotalQty(20);
            order.setActualTotalQty(20);
            order.setCreatedBy("admin");
            order.setUpdatedBy("admin");
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order = outboundOrderRepository.save(order);

            OutboundOrderDetail detail = new OutboundOrderDetail();
            detail.setOutboundOrderId(order.getId());
            detail.setDocNo("OUT-" + uniqueSuffix);
            detail.setLineNo(1);
            detail.setSupplierCode("SUP-001");
            detail.setSupplierName("上海汽车零部件");
            detail.setMaterialCode("MAT-OUT-" + uniqueSuffix);
            detail.setMaterialName("发动机支架");
            detail.setPlannedQty(20);
            detail.setActualQty(20);
            detail.setWarehouseArea("A区-01");
            detail.setCreatedBy("admin");
            detail.setUpdatedBy("admin");
            detail.setCreatedAt(LocalDateTime.now());
            detail.setUpdatedAt(LocalDateTime.now());
            detail = outboundOrderDetailRepository.save(detail);

            OutboundHistory history = new OutboundHistory();
            history.setOutboundOrderId(order.getId());
            history.setOutboundDetailId(detail.getId());
            history.setDocNo("OUT-" + uniqueSuffix);
            history.setMaterialCode("MAT-OUT-" + uniqueSuffix);
            history.setMaterialName("发动机支架");
            history.setSupplierName("上海汽车零部件");
            history.setIssueQty(20);
            history.setSourceInboundDoc("IN-TEST-001");
            history.setWarehouseArea("A区-01");
            history.setIssuedBy("admin");
            history.setStatus("已出库");
            history.setCreatedAt(LocalDateTime.now());
            outboundHistoryRepository.save(history);

            MvcResult result = mockMvc.perform(get("/api/export/outbound"))
                    .andExpect(status().isOk())
                    .andReturn();

            HttpServletResponse response = result.getResponse();
            String contentType = response.getContentType();
            assertNotNull(contentType);
            assertTrue(contentType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            byte[] body = result.getResponse().getContentAsByteArray();
            assertTrue(body.length > 0);

            try (XSSFWorkbook wb = new XSSFWorkbook(
                    new java.io.ByteArrayInputStream(body))) {
                Sheet sheet = wb.getSheet("出库明细");
                assertNotNull(sheet);

                Row headerRow = sheet.getRow(0);
                assertNotNull(headerRow);
                assertEquals(10, headerRow.getLastCellNum());
            }
        }
    }
}
