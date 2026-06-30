package com.example.wms.service.impl;

import com.example.wms.dto.inbound.*;
import com.example.wms.entity.InboundKanbanLabel;
import com.example.wms.entity.InventoryStock;
import com.example.wms.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 入库单服务层单元测试
 *
 * TC-IN-01: 创建入库单
 * TC-IN-02: 生成看板标签
 * TC-IN-03: 看板标签收货
 * TC-IN-04: 入库后库存增加
 * TC-IN-05: 重复收货抛异常
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("入库单服务测试")
public class InboundOrderServiceTest {

    @Autowired
    private InboundOrderServiceImpl inboundOrderService;

    @Autowired
    private InboundOrderRepository inboundOrderRepository;

    @Autowired
    private InboundOrderDetailRepository inboundOrderDetailRepository;

    @Autowired
    private InboundKanbanLabelRepository inboundKanbanLabelRepository;

    @Autowired
    private InventoryStockRepository inventoryStockRepository;

    @Autowired
    private DataSource dataSource;

    private static boolean schemaInitialized = false;

    @BeforeEach
    void initSchema() {
        if (!schemaInitialized) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("schema-h2.sql"));
            populator.setContinueOnError(true);
            populator.execute(dataSource);
            schemaInitialized = true;
        }
    }

    @AfterEach
    void cleanup() {
        inboundKanbanLabelRepository.deleteAll();
        inboundOrderDetailRepository.deleteAll();
        inboundOrderRepository.deleteAll();
        inventoryStockRepository.deleteAll();
    }

    // ==================== TC-IN-01: 创建入库单 ====================

    @Test
    @DisplayName("TC-IN-01: 创建入库单，成功返回单据号")
    void createOrder_shouldReturnDocNo() {
        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setTransferStatus("no-transfer");
        InboundOrderCreateDetailRequest detail = new InboundOrderCreateDetailRequest();
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setPlannedQty(100);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        InboundOrderDetailResponse response = inboundOrderService.createOrder(request, "admin");

        assertNotNull(response);
        assertNotNull(response.getOrder().getDocNo());
        assertTrue(response.getOrder().getDocNo().startsWith("IN"));
        assertEquals("Test Supplier", response.getOrder().getSupplier());
        assertEquals(100, response.getOrder().getPlannedTotalQty());
        assertEquals(1, response.getDetails().size());
    }

    @Test
    @DisplayName("TC-IN-01b: 物料列表为空应抛异常")
    void createOrder_emptyDetails_shouldThrow() {
        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setDetails(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> inboundOrderService.createOrder(request, "admin"));
    }

    // ==================== TC-IN-02: 生成看板标签 ====================

    @Test
    @DisplayName("TC-IN-02: 生成看板标签，返回标签列表")
    void generateKanbanLabels_shouldReturnLabels() {
        // 先创建入库单
        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setTransferStatus("no-transfer");
        InboundOrderCreateDetailRequest detail = new InboundOrderCreateDetailRequest();
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setPlannedQty(100);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        InboundOrderDetailResponse orderResp = inboundOrderService.createOrder(request, "admin");
        Long orderId = orderResp.getOrder().getId();

        // 生成看板标签（signature: Long id, String operator）
        List<InboundKanbanLabelDTO> labels = inboundOrderService.generateKanbanLabels(orderId, "admin");

        assertNotNull(labels);
        assertEquals(1, labels.size());
        assertNotNull(labels.get(0).getKanbanNo());
        assertTrue(labels.get(0).getKanbanNo().startsWith("R-"));
        assertEquals("MAT-001", labels.get(0).getMaterialCode());
        assertEquals(100, labels.get(0).getLabelQty());
        assertEquals("未入库", labels.get(0).getLabelStatus());
    }

    // ==================== TC-IN-03: 看板标签收货 ====================

    @Test
    @DisplayName("TC-IN-03: 看板标签收货，入库单状态变为已完成")
    void receiveByLabels_shouldCompleteOrder() {
        // 先创建入库单并生成标签
        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setTransferStatus("no-transfer");
        InboundOrderCreateDetailRequest detail = new InboundOrderCreateDetailRequest();
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setPlannedQty(100);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        InboundOrderDetailResponse orderResp = inboundOrderService.createOrder(request, "admin");
        Long orderId = orderResp.getOrder().getId();
        List<InboundKanbanLabelDTO> labels = inboundOrderService.generateKanbanLabels(orderId, "admin");
        Long labelId = labels.get(0).getId();

        // 收货（signature: Long id, List<Long> labelIds, String operator）
        InboundOrderDetailResponse receiveResp = inboundOrderService.receiveByLabels(orderId, List.of(labelId), "admin");

        assertEquals("已完成", receiveResp.getOrder().getStatus());
        assertEquals(100, receiveResp.getOrder().getActualTotalQty());
    }

    @Test
    @DisplayName("TC-IN-03b: 收货后标签状态变为已入库")
    void receiveByLabels_shouldUpdateLabelStatus() {
        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setTransferStatus("no-transfer");
        InboundOrderCreateDetailRequest detail = new InboundOrderCreateDetailRequest();
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setPlannedQty(100);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        InboundOrderDetailResponse orderResp = inboundOrderService.createOrder(request, "admin");
        Long orderId = orderResp.getOrder().getId();
        List<InboundKanbanLabelDTO> labels = inboundOrderService.generateKanbanLabels(orderId, "admin");
        Long labelId = labels.get(0).getId();

        inboundOrderService.receiveByLabels(orderId, List.of(labelId), "admin");

        InboundKanbanLabel updated = inboundKanbanLabelRepository.findById(labelId).orElseThrow();
        assertEquals("已入库", updated.getLabelStatus());
    }

    // ==================== TC-IN-04: 入库后库存增加 ====================

    @Test
    @DisplayName("TC-IN-04: 入库收货后，库存表新增记录")
    void receiveByLabels_shouldCreateInventoryStock() {
        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setTransferStatus("no-transfer");
        InboundOrderCreateDetailRequest detail = new InboundOrderCreateDetailRequest();
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setPlannedQty(100);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        InboundOrderDetailResponse orderResp = inboundOrderService.createOrder(request, "admin");
        Long orderId = orderResp.getOrder().getId();
        List<InboundKanbanLabelDTO> labels = inboundOrderService.generateKanbanLabels(orderId, "admin");
        Long labelId = labels.get(0).getId();

        inboundOrderService.receiveByLabels(orderId, List.of(labelId), "admin");

        List<InventoryStock> stocks = inventoryStockRepository.findAll();
        assertEquals(1, stocks.size());
        assertEquals("MAT-001", stocks.get(0).getMaterialCode());
        assertEquals(100, stocks.get(0).getOnHandQty());
    }

    // ==================== TC-IN-05: 重复收货 ====================

    @Test
    @DisplayName("TC-IN-05: 已完成的入库单不能重复收货")
    void receiveByLabels_alreadyCompleted_shouldThrow() {
        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setTransferStatus("no-transfer");
        InboundOrderCreateDetailRequest detail = new InboundOrderCreateDetailRequest();
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setPlannedQty(100);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        InboundOrderDetailResponse orderResp = inboundOrderService.createOrder(request, "admin");
        Long orderId = orderResp.getOrder().getId();
        List<InboundKanbanLabelDTO> labels = inboundOrderService.generateKanbanLabels(orderId, "admin");
        Long labelId = labels.get(0).getId();
        inboundOrderService.receiveByLabels(orderId, List.of(labelId), "admin");

        assertThrows(IllegalStateException.class,
                () -> inboundOrderService.receiveByLabels(orderId, List.of(labelId), "admin"));
    }
}
