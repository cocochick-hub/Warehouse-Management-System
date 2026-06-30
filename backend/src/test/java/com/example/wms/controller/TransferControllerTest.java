package com.example.wms.controller;

import com.example.wms.dto.transfer.TransferRequest;
import com.example.wms.entity.InboundKanbanLabel;
import com.example.wms.entity.InboundOrder;
import com.example.wms.entity.InboundOrderDetail;
import com.example.wms.repository.*;
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
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 看板转移（转包）Controller 层前后端联调测试
 * TC-FE-TRANS-01: 拆包（转移）
 * TC-FE-TRANS-02: 合包
 * TC-FE-TRANS-03: 自身合包应被拒绝
 * TC-FE-TRANS-04: 转移历史查询
 * TC-FE-TRANS-05: 看板查询
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("转包模块前后端联调测试")
public class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private InboundKanbanLabelRepository kanbanLabelRepository;

    @Autowired
    private InboundOrderRepository inboundOrderRepository;

    @Autowired
    private InboundOrderDetailRepository inboundOrderDetailRepository;

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
        // 清理残留数据（防止唯一约束冲突）
        kanbanLabelRepository.deleteAll();
        inboundOrderDetailRepository.deleteAll();
        inboundOrderRepository.deleteAll();
    }

    /**
     * 前置：创建已收货的看板标签，返回看板号
     */
    private String createReceivedKanban() {
        InboundOrder order = new InboundOrder();
        order.setDocNo("IN-TRANS-" + System.nanoTime());
        order.setSupplier("Transfer Test Supplier");
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
        detail.setDocNo(order.getDocNo());
        detail.setLineNo(1);
        detail.setSupplierCode("SUP-TRANS");
        detail.setSupplierName("Transfer Test Supplier");
        detail.setMaterialCode("MAT-TRANS");
        detail.setMaterialName("Transfer Test Material");
        detail.setPlannedQty(100);
        detail.setActualQty(100);
        detail.setPackageCount(1);
        detail.setWarehouseArea("WA-DEFAULT");
        detail.setCreatedBy("admin");
        detail.setUpdatedBy("admin");
        detail.setCreatedAt(LocalDateTime.now());
        detail.setUpdatedAt(LocalDateTime.now());
        detail = inboundOrderDetailRepository.save(detail);

        String kanbanNo = "R-TRANS-" + System.nanoTime();
        InboundKanbanLabel label = new InboundKanbanLabel();
        label.setKanbanNo(kanbanNo);
        label.setQrPayload("WMS-INBOUND|" + kanbanNo);
        label.setDocNo(order.getDocNo());
        label.setMaterialCode("MAT-TRANS");
        label.setMaterialName("Transfer Test Material");
        label.setSupplierCode("SUP-TRANS");
        label.setSupplierName("Transfer Test Supplier");
        label.setWarehouseArea("WA-DEFAULT");
        label.setLabelQty(100);
        label.setLabelStatus("已入库");
        label.setTransferStatus("不转包");
        label.setSealed(false);
        label.setInboundOrderId(order.getId());
        label.setInboundOrderDetailId(detail.getId());
        label.setPackageSeq(1);
        label.setPackageTotal(1);
        label.setCreatedBy("admin");
        label.setUpdatedBy("admin");
        label.setCreatedAt(LocalDateTime.now());
        label.setUpdatedAt(LocalDateTime.now());
        kanbanLabelRepository.save(label);

        return kanbanNo;
    }

    // ==================== TC-FE-TRANS-01: 拆包 ====================

    @Test
    @DisplayName("TC-FE-TRANS-01: 拆包转移30件，源剩余70，目标新建30")
    @WithMockUser(username = "admin", roles = {"admin"})
    void splitTransfer_shouldSplit30() throws Exception {
        String kanbanNo = createReceivedKanban();

        TransferRequest req = new TransferRequest();
        req.setSourceKanbanNo(kanbanNo);
        req.setTransferQty(30);

        mockMvc.perform(post("/api/transfer/execute")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sourceRemainingQty").value(70))
                .andExpect(jsonPath("$.data.targetQty").value(30))
                .andExpect(jsonPath("$.data.transferType").value("拆包"));
    }

    // ==================== TC-FE-TRANS-02: 合包 ====================

    @Test
    @DisplayName("TC-FE-TRANS-02: 合包，源转移至目标，目标数量增加")
    @WithMockUser(username = "admin", roles = {"admin"})
    void mergeTransfer_shouldMergeToTarget() throws Exception {
        String srcKanbanNo = createReceivedKanban();

        // 目标看板（已有30件）
        String tgtKanbanNo = createReceivedKanban();
        InboundKanbanLabel tgt = kanbanLabelRepository.findByKanbanNo(tgtKanbanNo).orElseThrow();
        tgt.setLabelQty(30);
        tgt.setTransferStatus("转入");
        kanbanLabelRepository.save(tgt);

        TransferRequest req = new TransferRequest();
        req.setSourceKanbanNo(srcKanbanNo);
        req.setTargetKanbanNo(tgtKanbanNo);
        req.setTransferQty(50);

        mockMvc.perform(post("/api/transfer/execute")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.transferType").value("合包"));
    }

    // ==================== TC-FE-TRANS-03: 自身合包 ====================

    @Test
    @DisplayName("TC-FE-TRANS-03: 自身合包应被拒绝")
    @WithMockUser(username = "admin", roles = {"admin"})
    void selfMerge_shouldReturn400() throws Exception {
        String kanbanNo = createReceivedKanban();

        TransferRequest req = new TransferRequest();
        req.setSourceKanbanNo(kanbanNo);
        req.setTargetKanbanNo(kanbanNo); // 自身
        req.setTransferQty(10);

        mockMvc.perform(post("/api/transfer/execute")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ==================== TC-FE-TRANS-04: 转移历史查询 ====================

    @Test
    @DisplayName("TC-FE-TRANS-04: 转移后查询历史记录")
    @WithMockUser(username = "admin", roles = {"admin"})
    void transferHistory_shouldRecord() throws Exception {
        String kanbanNo = createReceivedKanban();

        TransferRequest req = new TransferRequest();
        req.setSourceKanbanNo(kanbanNo);
        req.setTransferQty(20);

        mockMvc.perform(post("/api/transfer/execute")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/transfer/history")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    // ==================== TC-FE-TRANS-05: 看板查询 ====================

    @Test
    @DisplayName("TC-FE-TRANS-05: 按看板号查询标签详情")
    @WithMockUser(username = "admin", roles = {"admin"})
    void queryLabelByKanbanNo() throws Exception {
        String kanbanNo = createReceivedKanban();

        mockMvc.perform(get("/api/transfer/label")
                        .param("kanbanNo", kanbanNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.kanbanNo").value(kanbanNo))
                .andExpect(jsonPath("$.data.labelQty").value(100));
    }
}
