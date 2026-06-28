package com.example.wms.service.impl;

import com.example.wms.entity.InboundKanbanLabel;
import com.example.wms.entity.InboundOrder;
import com.example.wms.entity.InboundOrderDetail;
import com.example.wms.entity.PackageTransfer;
import com.example.wms.repository.InboundKanbanLabelRepository;
import com.example.wms.repository.InboundOrderDetailRepository;
import com.example.wms.repository.InboundOrderRepository;
import com.example.wms.repository.PackageTransferRepository;
import com.example.wms.dto.transfer.TransferRequest;
import com.example.wms.dto.transfer.TransferResultDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TransferServiceImpl 单元测试
 *
 * 测试覆盖：
 * - 向下转包（拆包）：源看板 → 新建看板
 * - 向上转包（合包）：源看板 → 已有看板
 * - 全量转包（源归零）
 * - 带余量转包
 * - 边界校验：封存、非入库状态、数量超限
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("转包服务测试")
public class TransferServiceImplTest {

    @Autowired
    private TransferServiceImpl transferService;

    @Autowired
    private InboundKanbanLabelRepository kanbanLabelRepository;

    @Autowired
    private PackageTransferRepository transferRepository;

    @Autowired
    private InboundOrderRepository inboundOrderRepository;

    @Autowired
    private InboundOrderDetailRepository inboundOrderDetailRepository;

    @Autowired
    private DataSource dataSource;

    private InboundKanbanLabel sourceLabel;

    @BeforeAll
    void initSchema() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema-h2.sql"));
        populator.setContinueOnError(true);
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        // 创建入库单（FK 依赖）
        InboundOrder order = new InboundOrder();
        order.setDocNo("IN-TEST-001");
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

        // 创建入库单明细（FK 依赖）
        InboundOrderDetail detail = new InboundOrderDetail();
        detail.setInboundOrderId(order.getId());
        detail.setDocNo("IN-TEST-001");
        detail.setLineNo(1);
        detail.setSupplierCode("SUP001");
        detail.setSupplierName("测试供应商");
        detail.setMaterialCode("MAT001");
        detail.setMaterialName("汽车轮毂");
        detail.setPackageModel("箱装");
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

        // 创建源看板：100件已入库的汽车轮毂
        sourceLabel = new InboundKanbanLabel();
        sourceLabel.setDocNo("IN-TEST-001");
        sourceLabel.setKanbanNo("R-20260628-IN001-MAT001-1-1");
        sourceLabel.setQrPayload("WMS-INBOUND|R-20260628-IN001-MAT001-1-1");
        sourceLabel.setMaterialCode("MAT001");
        sourceLabel.setMaterialName("汽车轮毂");
        sourceLabel.setSupplierCode("SUP001");
        sourceLabel.setSupplierName("测试供应商");
        sourceLabel.setPackageModel("箱装");
        sourceLabel.setWarehouseArea("A区-01");
        sourceLabel.setLabelQty(100);
        sourceLabel.setLabelStatus("已入库");
        sourceLabel.setTransferStatus("不转包");
        sourceLabel.setSealed(false);
        sourceLabel.setInboundOrderId(order.getId());
        sourceLabel.setInboundOrderDetailId(detail.getId());
        sourceLabel.setPackageSeq(1);
        sourceLabel.setPackageTotal(1);
        sourceLabel.setCreatedBy("admin");
        sourceLabel.setUpdatedBy("admin");
        sourceLabel.setCreatedAt(LocalDateTime.now());
        sourceLabel.setUpdatedAt(LocalDateTime.now());
        sourceLabel = kanbanLabelRepository.save(sourceLabel);
    }

    // ==================== 向下转包（拆包）====================

    @Nested
    @DisplayName("向下转包 - 源看板拆分到新建看板")
    class SplitTests {

        @Test
        @DisplayName("拆包30件，源保留70件")
        void shouldSplitAndKeepRemaining() {
            TransferRequest req = new TransferRequest();
            req.setSourceKanbanNo("R-20260628-IN001-MAT001-1-1");
            req.setTransferQty(30);
            req.setTargetKanbanNo(null); // 自动生成

            TransferResultDTO result = transferService.executeTransfer(req, "admin");

            assertNotNull(result);
            assertEquals(70, result.getSourceRemainingQty());
            assertEquals(30, result.getTargetQty());
            assertTrue(result.getTargetKanbanNo().startsWith("T-"));
            assertEquals("MAT001", result.getMaterialCode());

            // 验证源看板数量已更新
            InboundKanbanLabel updatedSource = kanbanLabelRepository.findByKanbanNo("R-20260628-IN001-MAT001-1-1").orElseThrow();
            assertEquals(70, updatedSource.getLabelQty());
            assertEquals("转包", updatedSource.getTransferStatus());

            // 验证目标看板已创建
            InboundKanbanLabel target = kanbanLabelRepository.findByKanbanNo(result.getTargetKanbanNo()).orElseThrow();
            assertEquals(30, target.getLabelQty());
            assertEquals("已入库", target.getLabelStatus());
            assertEquals("MAT001", target.getMaterialCode());
            assertEquals("转入", target.getTransferStatus());

            // 验证转包记录已生成
            java.util.List<com.example.wms.entity.PackageTransfer> records = transferRepository.findBySourceKanbanNoOrderByCreatedAtDesc("R-20260628-IN001-MAT001-1-1");
            assertEquals(1, records.size());
            assertEquals(30, records.get(0).getTransferQty());
            assertEquals(100, records.get(0).getSourceQtyBefore());
            assertEquals(70, records.get(0).getSourceQtyAfter());
        }

        @Test
        @DisplayName("全量转包100件，源归零")
        void shouldTransferAll() {
            TransferRequest req = new TransferRequest();
            req.setSourceKanbanNo("R-20260628-IN001-MAT001-1-1");
            req.setTransferQty(100);

            TransferResultDTO result = transferService.executeTransfer(req, "operator");

            assertEquals(0, result.getSourceRemainingQty());

            InboundKanbanLabel updatedSource = kanbanLabelRepository.findByKanbanNo("R-20260628-IN001-MAT001-1-1").orElseThrow();
            assertEquals(0, updatedSource.getLabelQty());
            assertEquals("已转包", updatedSource.getTransferStatus());
        }

        @Test
        @DisplayName("自定义目标看板号")
        void shouldUseCustomTargetKanbanNo() {
            TransferRequest req = new TransferRequest();
            req.setSourceKanbanNo("R-20260628-IN001-MAT001-1-1");
            req.setTransferQty(20);
            req.setTargetKanbanNo("T-MY-CUSTOM-001");

            TransferResultDTO result = transferService.executeTransfer(req, "admin");

            assertEquals("T-MY-CUSTOM-001", result.getTargetKanbanNo());
            assertTrue(kanbanLabelRepository.findByKanbanNo("T-MY-CUSTOM-001").isPresent());
        }
    }

    // ==================== 向上转包（合包）====================

    @Nested
    @DisplayName("向上转包 - 源看板汇入已有看板")
    class MergeTests {

        @Test
        @DisplayName("合包：源50件汇入目标已有30件 → 目标80件")
        void shouldMergeIntoExistingTarget() {
            // 创建一个已有目标看板
            InboundKanbanLabel targetLabel = new InboundKanbanLabel();
            targetLabel.setDocNo("IN-TEST-001");
            targetLabel.setKanbanNo("T-MERGE-TARGET");
            targetLabel.setQrPayload("WMS-INBOUND|T-MERGE-TARGET");
            targetLabel.setMaterialCode("MAT001");
            targetLabel.setMaterialName("汽车轮毂");
            targetLabel.setSupplierCode("SUP001");
            targetLabel.setSupplierName("测试供应商");
            targetLabel.setPackageModel("箱装");
            targetLabel.setWarehouseArea("A区-01");
            targetLabel.setLabelQty(30);
            targetLabel.setLabelStatus("已入库");
            targetLabel.setTransferStatus("转入");
            targetLabel.setSealed(false);
            targetLabel.setInboundOrderId(sourceLabel.getInboundOrderId());
            targetLabel.setInboundOrderDetailId(sourceLabel.getInboundOrderDetailId());
            targetLabel.setPackageSeq(1);
            targetLabel.setPackageTotal(1);
            targetLabel.setCreatedBy("admin");
            targetLabel.setUpdatedBy("admin");
            targetLabel.setCreatedAt(LocalDateTime.now());
            targetLabel.setUpdatedAt(LocalDateTime.now());
            kanbanLabelRepository.save(targetLabel);

            // 执行合包
            TransferRequest req = new TransferRequest();
            req.setSourceKanbanNo("R-20260628-IN001-MAT001-1-1");
            req.setTargetKanbanNo("T-MERGE-TARGET");
            req.setTransferQty(50);

            TransferResultDTO result = transferService.executeTransfer(req, "admin");

            assertEquals(50, result.getSourceRemainingQty());
            assertEquals(80, result.getTargetQty()); // 30 + 50

            InboundKanbanLabel updatedTarget = kanbanLabelRepository.findByKanbanNo("T-MERGE-TARGET").orElseThrow();
            assertEquals(80, updatedTarget.getLabelQty());
            assertEquals("转入", updatedTarget.getTransferStatus());
        }

        @Test
        @DisplayName("合包送不同物料号，应抛异常")
        void shouldRejectMergeDifferentMaterial() {
            // 创建目标看板，物料号不同
            InboundKanbanLabel otherMaterial = new InboundKanbanLabel();
            otherMaterial.setDocNo("IN-TEST-001");
            otherMaterial.setKanbanNo("T-OTHER-MAT");
            otherMaterial.setQrPayload("WMS-INBOUND|T-OTHER-MAT");
            otherMaterial.setMaterialCode("MAT002"); // ← 不同！
            otherMaterial.setMaterialName("刹车片");
            otherMaterial.setSupplierCode(sourceLabel.getSupplierCode());
            otherMaterial.setSupplierName(sourceLabel.getSupplierName());
            otherMaterial.setLabelQty(30);
            otherMaterial.setLabelStatus("已入库");
            otherMaterial.setSealed(false);
            otherMaterial.setInboundOrderId(sourceLabel.getInboundOrderId());
            otherMaterial.setInboundOrderDetailId(sourceLabel.getInboundOrderDetailId());
            otherMaterial.setPackageSeq(1);
            otherMaterial.setPackageTotal(1);
            otherMaterial.setCreatedBy("admin");
            otherMaterial.setUpdatedBy("admin");
            otherMaterial.setCreatedAt(LocalDateTime.now());
            otherMaterial.setUpdatedAt(LocalDateTime.now());
            kanbanLabelRepository.save(otherMaterial);

            TransferRequest req = new TransferRequest();
            req.setSourceKanbanNo("R-20260628-IN001-MAT001-1-1");
            req.setTargetKanbanNo("T-OTHER-MAT");
            req.setTransferQty(10);

            assertThrows(IllegalArgumentException.class,
                    () -> transferService.executeTransfer(req, "admin"),
                    "物料号不一致应抛异常");
        }
    }

    // ==================== 校验 ====================

    @Nested
    @DisplayName("校验 - 非法操作应拒绝")
    class ValidationTests {

        @Test
        @DisplayName("封存的看板不能转包")
        void shouldRejectSealedKanban() {
            sourceLabel.setSealed(true);
            kanbanLabelRepository.save(sourceLabel);

            TransferRequest req = new TransferRequest();
            req.setSourceKanbanNo("R-20260628-IN001-MAT001-1-1");
            req.setTransferQty(10);

            assertThrows(IllegalStateException.class,
                    () -> transferService.executeTransfer(req, "admin"),
                    "封存看板应拒绝转包");
        }

        @Test
        @DisplayName("非入库状态的看板不能转包")
        void shouldRejectNonReceivedKanban() {
            sourceLabel.setLabelStatus("未入库");
            kanbanLabelRepository.save(sourceLabel);

            TransferRequest req = new TransferRequest();
            req.setSourceKanbanNo("R-20260628-IN001-MAT001-1-1");
            req.setTransferQty(10);

            assertThrows(IllegalStateException.class,
                    () -> transferService.executeTransfer(req, "admin"),
                    "未入库看板应拒绝转包");
        }

        @Test
        @DisplayName("转移数量超限应拒绝")
        void shouldRejectExcessQty() {
            TransferRequest req = new TransferRequest();
            req.setSourceKanbanNo("R-20260628-IN001-MAT001-1-1");
            req.setTransferQty(101); // 源只有100件

            assertThrows(IllegalArgumentException.class,
                    () -> transferService.executeTransfer(req, "admin"),
                    "超过源数量的转移应拒绝");
        }

        @Test
        @DisplayName("转移数量为0或负应拒绝")
        void shouldRejectInvalidQty() {
            TransferRequest req = new TransferRequest();
            req.setSourceKanbanNo("R-20260628-IN001-MAT001-1-1");
            req.setTransferQty(0);

            assertThrows(IllegalArgumentException.class,
                    () -> transferService.executeTransfer(req, "admin"));
        }

        @Test
        @DisplayName("不存在的看板号应抛EntityNotFoundException")
        void shouldThrowForMissingKanban() {
            TransferRequest req = new TransferRequest();
            req.setSourceKanbanNo("R-NOT-EXIST");
            req.setTransferQty(10);

            assertThrows(EntityNotFoundException.class,
                    () -> transferService.executeTransfer(req, "admin"));
        }
    }

    // ==================== 事务 ====================

    @Nested
    @DisplayName("事务 - 原子性验证")
    class TransactionTests {

        @Test
        @DisplayName("合法转包：源更新、目标创建、记录写入三者同步完成")
        void shouldBeAtomic() {
            TransferRequest req = new TransferRequest();
            req.setSourceKanbanNo("R-20260628-IN001-MAT001-1-1");
            req.setTransferQty(25);

            TransferResultDTO result = transferService.executeTransfer(req, "admin");

            // 三者都完成
            assertEquals(75, result.getSourceRemainingQty());
            assertNotNull(result.getTargetKanbanNo());
            assertEquals(25, result.getTargetQty());

            InboundKanbanLabel source = kanbanLabelRepository.findByKanbanNo("R-20260628-IN001-MAT001-1-1").orElseThrow();
            assertEquals(75, source.getLabelQty());

            InboundKanbanLabel target = kanbanLabelRepository.findByKanbanNo(result.getTargetKanbanNo()).orElseThrow();
            assertEquals(25, target.getLabelQty());

            java.util.List<com.example.wms.entity.PackageTransfer> records = transferRepository.findBySourceKanbanNoOrderByCreatedAtDesc("R-20260628-IN001-MAT001-1-1");
            assertEquals(1, records.size());

            // 数量关系正确
            assertEquals(100 - 25, source.getLabelQty());
            assertEquals(25, target.getLabelQty());
        }
    }
}
