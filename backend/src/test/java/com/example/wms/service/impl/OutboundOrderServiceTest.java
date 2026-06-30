package com.example.wms.service.impl;

import com.example.wms.dto.inbound.InboundOrderCreateDetailRequest;
import com.example.wms.dto.inbound.InboundOrderCreateRequest;
import com.example.wms.dto.inbound.InboundOrderDetailResponse;
import com.example.wms.dto.inbound.InboundKanbanLabelDTO;
import com.example.wms.dto.outbound.*;
import com.example.wms.entity.*;
import com.example.wms.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 出库单服务层单元测试
 *
 * TC-OUT-01: 创建出库单
 * TC-OUT-02: 入库+出库全流程
 * TC-OUT-03: 出库数量不能超过待出数量
 * TC-OUT-04: 部分出库（labelIssueQtys），库存扣减正确
 * TC-OUT-05: 已全量出库的标签不能重复出库
 * TC-OUT-06: 出库后标签状态变为已出库
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
@DisplayName("出库单服务测试")
public class OutboundOrderServiceTest {

    @Autowired
    private OutboundOrderServiceImpl outboundOrderService;

    @Autowired
    private InboundOrderServiceImpl inboundOrderService;

    @Autowired
    private InboundOrderRepository inboundOrderRepository;

    @Autowired
    private InboundOrderDetailRepository inboundOrderDetailRepository;

    @Autowired
    private InboundKanbanLabelRepository inboundKanbanLabelRepository;

    @Autowired
    private OutboundOrderRepository outboundOrderRepository;

    @Autowired
    private OutboundOrderDetailRepository outboundOrderDetailRepository;

    @Autowired
    private OutboundHistoryRepository outboundHistoryRepository;

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
        outboundHistoryRepository.deleteAll();
        outboundOrderDetailRepository.deleteAll();
        outboundOrderRepository.deleteAll();
        inboundKanbanLabelRepository.deleteAll();
        inboundOrderDetailRepository.deleteAll();
        inboundOrderRepository.deleteAll();
        inventoryStockRepository.deleteAll();
    }

    // ==================== TC-OUT-01: 创建出库单 ====================

    @Test
    @DisplayName("TC-OUT-01: 创建出库单，成功返回单据号")
    void createOrder_shouldReturnDocNo() {
        OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setOutboundType("type-a");
        OutboundCreateDetailRequest detail = new OutboundCreateDetailRequest();
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setPlannedQty(10);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        OutboundOrderDetailResponse response = outboundOrderService.createOrder(request, "admin");

        assertNotNull(response);
        assertNotNull(response.getOrder().getDocNo());
        assertTrue(response.getOrder().getDocNo().startsWith("OUT"));
        assertEquals("待出库", response.getOrder().getStatus());
        assertEquals(10, response.getOrder().getPlannedTotalQty());
    }

    @Test
    @DisplayName("TC-OUT-01b: 物料列表为空应抛异常")
    void createOrder_emptyDetails_shouldThrow() {
        OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setOutboundType("type-a");
        request.setDetails(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> outboundOrderService.createOrder(request, "admin"));
    }

    // ==================== TC-OUT-02: 入库+出库全流程 ====================

    /**
     * 创建已收货入库单的辅助方法
     * 返回 Long[]{orderId, labelId}
     */
    private Long[] createReceivedInboundOrder(int inboundQty) {
        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setTransferStatus("no-transfer");
        InboundOrderCreateDetailRequest detail = new InboundOrderCreateDetailRequest();
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setPlannedQty(inboundQty);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        InboundOrderDetailResponse orderResp = inboundOrderService.createOrder(request, "admin");
        Long orderId = orderResp.getOrder().getId();
        List<InboundKanbanLabelDTO> labels = inboundOrderService.generateKanbanLabels(orderId, "admin");
        Long labelId = labels.get(0).getId();
        inboundOrderService.receiveByLabels(orderId, List.of(labelId), "admin");
        return new Long[]{orderId, labelId};
    }

    @Test
    @DisplayName("TC-OUT-02: 入库100，出库10，库存剩余90")
    void fullFlow_inbound100_outbound10_stock90() {
        // 入库100
        Long[] inbound = createReceivedInboundOrder(100);
        assertEquals(100, inventoryStockRepository.findAll().get(0).getOnHandQty());

        // 创建出库单
        OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setOutboundType("type-a");
        OutboundCreateDetailRequest detail = new OutboundCreateDetailRequest();
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setPlannedQty(10);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        OutboundOrderDetailResponse outboundResp = outboundOrderService.createOrder(request, "admin");
        Long outboundOrderId = outboundResp.getOrder().getId();
        Long labelId = inbound[1];

        // 查询可用标签
        List<OutboundKanbanLabelDTO> availableLabels = outboundOrderService.getAvailableKanbanLabels(outboundOrderId);
        assertFalse(availableLabels.isEmpty());

        // 出库（部分出库10）
        OutboundOrderDetailResponse issueResp = outboundOrderService.issueByLabels(
                outboundOrderId, List.of(labelId), Map.of(labelId, 10), "admin");

        assertEquals("已完成", issueResp.getOrder().getStatus());
        assertEquals(10, issueResp.getOrder().getActualTotalQty());

        // 验证库存
        InventoryStock stock = inventoryStockRepository.findAll().get(0);
        assertEquals(90, stock.getOnHandQty());
    }

    // ==================== TC-OUT-03: 出库数量不能超过待出数量 ====================

    @Test
    @DisplayName("TC-OUT-03: 出库数量超过待出数量应被拒绝")
    void issueByLabels_overPending_shouldThrow() {
        // 入库100
        Long[] inbound = createReceivedInboundOrder(100);

        // 创建出库单，计划5
        OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setOutboundType("type-a");
        OutboundCreateDetailRequest detail = new OutboundCreateDetailRequest();
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setPlannedQty(5);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        OutboundOrderDetailResponse outboundResp = outboundOrderService.createOrder(request, "admin");
        Long outboundOrderId = outboundResp.getOrder().getId();
        Long labelId = inbound[1];

        // 尝试出库20（超过计划的5）
        assertThrows(IllegalArgumentException.class, () ->
                outboundOrderService.issueByLabels(outboundOrderId, List.of(labelId), Map.of(labelId, 20), "admin"));
    }

    // ==================== TC-OUT-04: 部分出库 ====================

    @Test
    @DisplayName("TC-OUT-04: 标签有100可用，出库10，库存扣10，标签剩余90（未全出）")
    void issueByLabels_partial_shouldDeductOnly10() {
        // 入库100
        Long[] inbound = createReceivedInboundOrder(100);

        // 创建出库单，计划10
        OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setOutboundType("type-a");
        OutboundCreateDetailRequest detail = new OutboundCreateDetailRequest();
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setPlannedQty(10);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        OutboundOrderDetailResponse outboundResp = outboundOrderService.createOrder(request, "admin");
        Long outboundOrderId = outboundResp.getOrder().getId();
        Long labelId = inbound[1];

        // 部分出库：只出10
        OutboundOrderDetailResponse issueResp = outboundOrderService.issueByLabels(
                outboundOrderId, List.of(labelId), Map.of(labelId, 10), "admin");

        assertEquals("已完成", issueResp.getOrder().getStatus());

        // 库存剩余90
        InventoryStock stock = inventoryStockRepository.findAll().get(0);
        assertEquals(90, stock.getOnHandQty());

        // 标签应被修改（部分出库，labelQty从100变成90，状态仍为不转包）
        var updatedLabel = inboundKanbanLabelRepository.findById(labelId).orElseThrow();
        assertEquals(90, updatedLabel.getLabelQty());
        assertEquals("不转包", updatedLabel.getTransferStatus());
    }

    // ==================== TC-OUT-05: 标签全量出库 ====================

    @Test
    @DisplayName("TC-OUT-05: 按标签全量出库，标签状态变为已出库")
    void issueByLabels_full_shouldMarkLabelIssued() {
        // 入库100
        Long[] inbound = createReceivedInboundOrder(100);

        // 创建出库单，计划100
        OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setOutboundType("type-a");
        OutboundCreateDetailRequest detail = new OutboundCreateDetailRequest();
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setPlannedQty(100);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        OutboundOrderDetailResponse outboundResp = outboundOrderService.createOrder(request, "admin");
        Long outboundOrderId = outboundResp.getOrder().getId();
        Long labelId = inbound[1];

        // 全量出库（不传labelIssueQtys，按标签全部labelQty=100出）
        OutboundOrderDetailResponse issueResp = outboundOrderService.issueByLabels(
                outboundOrderId, List.of(labelId), null, "admin");

        // 标签状态应变为已出库
        var updatedLabel = inboundKanbanLabelRepository.findById(labelId).orElseThrow();
        assertEquals("已出库", updatedLabel.getTransferStatus());
    }

    // ==================== TC-OUT-06: 出库后出库历史记录 ====================

    @Test
    @DisplayName("TC-OUT-06: 出库后生成出库历史记录")
    void issueByLabels_shouldCreateHistoryRecord() {
        Long[] inbound = createReceivedInboundOrder(100);

        OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
        request.setSupplier("Test Supplier");
        request.setOutboundType("type-a");
        OutboundCreateDetailRequest detail = new OutboundCreateDetailRequest();
        detail.setMaterialCode("MAT-001");
        detail.setMaterialName("Test Material");
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Test Supplier");
        detail.setPlannedQty(10);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        OutboundOrderDetailResponse outboundResp = outboundOrderService.createOrder(request, "admin");
        Long outboundOrderId = outboundResp.getOrder().getId();
        Long labelId = inbound[1];

        outboundOrderService.issueByLabels(outboundOrderId, List.of(labelId), Map.of(labelId, 10), "admin");

        List<OutboundHistory> histories = outboundHistoryRepository.findAll();
        assertEquals(1, histories.size());
        assertEquals("MAT-001", histories.get(0).getMaterialCode());
        assertEquals(10, histories.get(0).getIssueQty());
    }
}
