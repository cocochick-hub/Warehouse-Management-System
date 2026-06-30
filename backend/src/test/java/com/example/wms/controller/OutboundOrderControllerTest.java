package com.example.wms.controller;

import com.example.wms.dto.inbound.InboundOrderCreateDetailRequest;
import com.example.wms.dto.inbound.InboundOrderCreateRequest;
import com.example.wms.dto.outbound.OutboundOrderCreateRequest;
import com.example.wms.dto.outbound.OutboundCreateDetailRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
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

import javax.sql.DataSource;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 出库模块 Controller 层前后端联调测试
 * TC-FE-OUT-01: 入库+创建出库单→查询可用标签→部分出库
 * TC-FE-OUT-02: 出库单分页查询
 * TC-FE-OUT-03: 出库数量不能超过待出
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("出库模块前后端联调测试")
public class OutboundOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    /**
     * 前置：创建已收货入库单，返回 {inboundOrderId, labelId}
     */
    private long[] createReceivedInboundOrder(int qty) throws Exception {
        InboundOrderCreateRequest req = new InboundOrderCreateRequest();
        req.setSupplier("Outbound FE Supplier");
        req.setTransferStatus("no-transfer");
        InboundOrderCreateDetailRequest detail = new InboundOrderCreateDetailRequest();
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Outbound FE Supplier");
        detail.setMaterialCode("MAT-OUT-001");
        detail.setMaterialName("Outbound FE Material");
        detail.setPlannedQty(qty);
        detail.setWarehouseArea("WA-DEFAULT");
        req.setDetails(List.of(detail));

        MvcResult r = mockMvc.perform(post("/api/inbound/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        long orderId = objectMapper.readTree(r.getResponse().getContentAsString())
                .get("data").get("order").get("id").asLong();

        MvcResult labelR = mockMvc.perform(post("/api/inbound/orders/{id}/kanban-labels/generate", orderId)
                        .contentType("application/json")
                        .content("{\"warehouseArea\":\"WA-DEFAULT\"}"))
                .andExpect(status().isOk())
                .andReturn();

        long labelId = objectMapper.readTree(labelR.getResponse().getContentAsString())
                .get("data").get(0).get("id").asLong();

        mockMvc.perform(post("/api/inbound/orders/{id}/receive-by-labels", orderId)
                        .contentType("application/json")
                        .content(String.format("{\"labelIds\":[%d]}", labelId)))
                .andExpect(status().isOk());

        return new long[]{orderId, labelId};
    }

    // ==================== TC-FE-OUT-01: 出库全流程 ====================

    @Test
    @DisplayName("TC-FE-OUT-01: 入库→创建出库单→查询可用标签→部分出库10")
    @WithMockUser(username = "admin", roles = {"admin"})
    void fullOutboundFlow_partial10() throws Exception {
        int inboundQty = 100;
        long[] inbound = createReceivedInboundOrder(inboundQty);
        long labelId = inbound[1];

        // 创建出库单，计划出10
        OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
        request.setSupplier("Outbound FE Supplier");
        request.setOutboundType("type-a");
        OutboundCreateDetailRequest detail = new OutboundCreateDetailRequest();
        detail.setMaterialCode("MAT-OUT-001");
        detail.setMaterialName("Outbound FE Material");
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Outbound FE Supplier");
        detail.setPlannedQty(10);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        MvcResult createResult = mockMvc.perform(post("/api/outbound/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.order.docNo").isNotEmpty())
                .andReturn();

        long outboundOrderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("order").get("id").asLong();

        // 查询可用标签
        mockMvc.perform(get("/api/outbound/orders/{id}/available-kanban-labels", outboundOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].availableQty").value(100));

        // 部分出库10（使用 labelIssueQtys）
        String issueBody = String.format(
                "{\"labelIds\":[%d],\"labelIssueQtys\":{\"%d\":10}}", labelId, labelId);
        mockMvc.perform(post("/api/outbound/orders/{id}/issue-by-labels", outboundOrderId)
                        .contentType("application/json")
                        .content(issueBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.order.status").value("已完成"))
                .andExpect(jsonPath("$.data.order.actualTotalQty").value(10));
    }

    // ==================== TC-FE-OUT-02: 分页查询 ====================

    @Test
    @DisplayName("TC-FE-OUT-02: 分页查询出库单列表")
    @WithMockUser(username = "admin", roles = {"admin"})
    void listOrders_shouldReturnPagedResult() throws Exception {
        mockMvc.perform(get("/api/outbound/orders")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(1));
    }

    // ==================== TC-FE-OUT-03: 出库数量不能超过待出 ====================

    @Test
    @DisplayName("TC-FE-OUT-03: 出库数量超过待出数量返回400")
    @WithMockUser(username = "admin", roles = {"admin"})
    void outboundOverPending_shouldReturn400() throws Exception {
        long[] inbound = createReceivedInboundOrder(100);
        long labelId = inbound[1];

        // 创建出库单，计划5
        OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
        request.setSupplier("Outbound FE Supplier");
        request.setOutboundType("type-a");
        OutboundCreateDetailRequest detail = new OutboundCreateDetailRequest();
        detail.setMaterialCode("MAT-OUT-001");
        detail.setMaterialName("Outbound FE Material");
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Outbound FE Supplier");
        detail.setPlannedQty(5);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        MvcResult createResult = mockMvc.perform(post("/api/outbound/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        long outboundOrderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("order").get("id").asLong();

        // 尝试出库20（超过计划的5）
        String issueBody = String.format(
                "{\"labelIds\":[%d],\"labelIssueQtys\":{\"%d\":20}}", labelId, labelId);
        mockMvc.perform(post("/api/outbound/orders/{id}/issue-by-labels", outboundOrderId)
                        .contentType("application/json")
                        .content(issueBody))
                .andExpect(status().isBadRequest());
    }
}
