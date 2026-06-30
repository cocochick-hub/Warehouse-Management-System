package com.example.wms.controller;

import com.example.wms.dto.inbound.InboundOrderCreateRequest;
import com.example.wms.dto.inbound.InboundOrderCreateDetailRequest;
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
 * 入库模块 Controller 层前后端联调测试
 * 模拟前端 Vue/React 发起的完整 HTTP 请求
 *
 * TC-FE-IN-01: 创建入库单 → 生成标签 → 收货
 * TC-FE-IN-02: 入库单分页查询
 * TC-FE-IN-03: 入库单详情查询
 * TC-FE-IN-04: 无效token应返回401
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("入库模块前后端联调测试")
public class InboundOrderControllerTest {

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

    // ==================== TC-FE-IN-01: 入库全流程 ====================

    @Test
    @DisplayName("TC-FE-IN-01: 创建入库单→生成标签→收货")
    @WithMockUser(username = "admin", roles = {"admin"})
    void fullInboundFlow() throws Exception {
        // Step 1: 创建入库单
        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplier("FE Test Supplier");
        request.setTransferStatus("no-transfer");
        InboundOrderCreateDetailRequest detail = new InboundOrderCreateDetailRequest();
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("FE Test Supplier");
        detail.setMaterialCode("MAT-FE-001");
        detail.setMaterialName("FE Test Material");
        detail.setPlannedQty(50);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        MvcResult orderResult = mockMvc.perform(post("/api/inbound/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.order.docNo").isNotEmpty())
                .andReturn();

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .get("data").get("order").get("id").asLong();

        // Step 2: 生成看板标签
        MvcResult labelResult = mockMvc.perform(post("/api/inbound/orders/{id}/kanban-labels/generate", orderId)
                        .contentType("application/json")
                        .content("{\"warehouseArea\":\"WA-DEFAULT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].kanbanNo").isNotEmpty())
                .andReturn();

        Long labelId = objectMapper.readTree(labelResult.getResponse().getContentAsString())
                .get("data").get(0).get("id").asLong();

        // Step 3: 收货
        String receiveBody = String.format(
                "{\"labelIds\":[%d]}", labelId);
        mockMvc.perform(post("/api/inbound/orders/{id}/receive-by-labels", orderId)
                        .contentType("application/json")
                        .content(receiveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.order.status").value("已完成"))
                .andExpect(jsonPath("$.data.order.actualTotalQty").value(50));
    }

    // ==================== TC-FE-IN-02: 分页查询 ====================

    @Test
    @DisplayName("TC-FE-IN-02: 分页查询入库单列表")
    @WithMockUser(username = "admin", roles = {"admin"})
    void listOrders_shouldReturnPagedResult() throws Exception {
        mockMvc.perform(get("/api/inbound/orders")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.page").value(1));
    }

    // ==================== TC-FE-IN-03: 详情查询 ====================

    @Test
    @DisplayName("TC-FE-IN-03: 查询入库单详情（含明细）")
    @WithMockUser(username = "admin", roles = {"admin"})
    void getOrderDetail_shouldReturnWithDetails() throws Exception {
        // 先创建一个入库单
        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplier("Detail Test");
        request.setTransferStatus("no-transfer");
        InboundOrderCreateDetailRequest detail = new InboundOrderCreateDetailRequest();
        detail.setSupplierCode("SUP-001");
        detail.setSupplierName("Detail Test");
        detail.setMaterialCode("MAT-DETAIL-001");
        detail.setMaterialName("Detail Material");
        detail.setPlannedQty(20);
        detail.setWarehouseArea("WA-DEFAULT");
        request.setDetails(List.of(detail));

        MvcResult createResult = mockMvc.perform(post("/api/inbound/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Long orderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("order").get("id").asLong();

        // 查询详情
        mockMvc.perform(get("/api/inbound/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.order.id").value(orderId))
                .andExpect(jsonPath("$.data.details").isArray());
    }

    // ==================== TC-FE-IN-04: 未认证应返回401/403 ====================

    @Test
    @DisplayName("TC-FE-IN-04: 未认证请求应返回401（无token）")
    void noAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/inbound/orders"))
                .andExpect(status().isUnauthorized());
    }
}
