package com.example.wms.aspect;

import com.example.wms.dto.basic.WarehouseAreaDTO;
import com.example.wms.entity.AuditLog;
import com.example.wms.entity.InboundKanbanLabel;
import com.example.wms.entity.InboundOrder;
import com.example.wms.entity.InboundOrderDetail;
import com.example.wms.repository.AuditLogRepository;
import com.example.wms.repository.InboundKanbanLabelRepository;
import com.example.wms.repository.InboundOrderDetailRepository;
import com.example.wms.repository.InboundOrderRepository;
import com.example.wms.repository.WarehouseAreaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuditLogAspect 切面单元测试
 *
 * TC-A-01：POST /api/inbound/orders → CREATE 日志
 * TC-A-02：PUT /api/basic/warehouse-areas/{id} → UPDATE 日志
 * TC-A-03：DELETE /api/basic/warehouse-areas/{id} → DELETE 日志
 * TC-A-04：匿名用户请求返回 401，不写审计日志
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("操作审计日志切面测试")
public class AuditLogAspectTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private InboundOrderRepository inboundOrderRepository;

    @Autowired
    private InboundOrderDetailRepository inboundOrderDetailRepository;

    @Autowired
    private InboundKanbanLabelRepository kanbanLabelRepository;

    @Autowired
    private WarehouseAreaRepository warehouseAreaRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void initSchema() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema-h2.sql"));
        populator.setContinueOnError(true);
        populator.execute(dataSource);
    }

    private void cleanAuditLogs() {
        auditLogRepository.deleteAll();
    }

    // ==================== TC-A-01：POST → CREATE ====================

    @Nested
    @DisplayName("TC-A-01：POST 请求写入 CREATE 日志")
    class PostCreatesLog {

        @Test
        @DisplayName("POST /api/inbound/orders 创建入库单，记录 action=CREATE")
        @WithMockUser(username = "admin", roles = {"admin"})
        void postInboundOrder_shouldLogCreate() throws Exception {
            cleanAuditLogs();

            String requestBody = """
                {
                    "supplier": "审计测试供应商",
                    "transferStatus": "不转包",
                    "details": [{
                        "supplierCode": "SUP-001",
                        "supplierName": "上海汽车零部件",
                        "materialCode": "MAT-AUDIT-001",
                        "materialName": "测试物料",
                        "plannedQty": 100,
                        "warehouseArea": "A区-01"
                    }]
                }
                """;

            mockMvc.perform(post("/api/inbound/orders")
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isOk());

            var logs = auditLogRepository.findAll();
            AuditLog createLog = logs.stream()
                    .filter(l -> "CREATE".equals(l.getAction()))
                    .filter(l -> l.getDetail() != null && l.getDetail().contains("审计测试供应商"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(createLog, "应有 CREATE 类型的审计日志");
            assertEquals("InboundOrder", createLog.getTarget());
            assertEquals("admin", createLog.getUsername());
            assertNotNull(createLog.getDetail());
        }
    }

    // ==================== TC-A-02：PUT → UPDATE ====================

    @Nested
    @DisplayName("TC-A-02：PUT 请求写入 UPDATE 日志")
    class PutUpdatesLog {

        @Test
        @DisplayName("PUT /api/basic/warehouse-areas/{id} 更新库区，记录 action=UPDATE")
        @WithMockUser(username = "operator", roles = {"operator"})
        void putWarehouseArea_shouldLogUpdate() throws Exception {
            cleanAuditLogs();

            // schema-h2.sql 中已有初始数据，使用 WA-DEFAULT (id=1)
            Long areaId = 1L;

            String updateBody = """
                {
                    "id": 1,
                    "areaCode": "WA-DEFAULT",
                    "areaName": "默认库区（已更新）",
                    "sortOrder": 10,
                    "description": "更新后的描述"
                }
                """;

            mockMvc.perform(put("/api/basic/warehouse-areas/{id}", areaId)
                            .contentType("application/json")
                            .content(updateBody))
                    .andExpect(status().isOk());

            var logs = auditLogRepository.findAll();
            AuditLog updateLog = logs.stream()
                    .filter(l -> "UPDATE".equals(l.getAction()))
                    .filter(l -> l.getDetail() != null && l.getDetail().contains("WA-DEFAULT"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(updateLog, "应有 UPDATE 类型的审计日志");
            assertEquals("BasicData", updateLog.getTarget());
            assertEquals("operator", updateLog.getUsername());
        }
    }

    // ==================== TC-A-03：DELETE → DELETE ====================

    @Nested
    @DisplayName("TC-A-03：DELETE 请求写入 DELETE 日志")
    class DeleteDeletesLog {

        @Test
        @DisplayName("DELETE /api/basic/warehouse-areas/{id} 删除库区，记录 action=DELETE")
        @WithMockUser(username = "admin", roles = {"admin"})
        void deleteWarehouseArea_shouldLogDelete() throws Exception {
            cleanAuditLogs();

            // 创建新库区用于删除测试（避免删除初始数据）
            com.example.wms.entity.WarehouseArea area = new com.example.wms.entity.WarehouseArea();
            area.setAreaCode("WA-DELETE-TEST");
            area.setAreaName("删除测试库区");
            area.setSortOrder(99);
            area.setDescription("用于删除测试");
            area.setCreatedBy("admin");
            area.setUpdatedBy("admin");
            area.setCreatedAt(LocalDateTime.now());
            area.setUpdatedAt(LocalDateTime.now());
            area = warehouseAreaRepository.save(area);

            Long areaId = area.getId();

            mockMvc.perform(delete("/api/basic/warehouse-areas/{id}", areaId))
                    .andExpect(status().isOk());

            var logs = auditLogRepository.findAll();
            AuditLog deleteLog = logs.stream()
                    .filter(l -> "DELETE".equals(l.getAction()))
                    .filter(l -> l.getDetail() != null)
                    .findFirst()
                    .orElse(null);

            assertNotNull(deleteLog, "应有 DELETE 类型的审计日志");
            assertEquals("BasicData", deleteLog.getTarget());
            assertEquals("admin", deleteLog.getUsername());
        }
    }

    // ==================== TC-A-04：匿名用户 ====================

    @Nested
    @DisplayName("TC-A-04：匿名用户请求返回 401")
    class AnonymousUser {

        @Test
        @DisplayName("匿名用户 POST 请求返回 401，不写审计日志")
        void anonymousPost_shouldReturn401() throws Exception {
            cleanAuditLogs();

            String requestBody = """
                {
                    "supplier": "匿名测试",
                    "details": [{
                        "supplierCode": "SUP-001",
                        "supplierName": "上海汽车零部件",
                        "materialCode": "MAT-ANON-001",
                        "materialName": "匿名测试物料",
                        "plannedQty": 10,
                        "warehouseArea": "A区-01"
                    }]
                }
                """;

            // 匿名用户被 JWT 过滤器拦截
            mockMvc.perform(post("/api/inbound/orders")
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());

            var logs = auditLogRepository.findAll();
            assertTrue(logs.isEmpty(), "匿名请求不应写入审计日志");
        }
    }
}
