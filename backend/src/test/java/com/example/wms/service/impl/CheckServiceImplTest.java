package com.example.wms.service.impl;

import com.example.wms.dto.check.CheckDetailDTO;
import com.example.wms.dto.check.CheckProgressDTO;
import com.example.wms.dto.check.CheckTaskDTO;
import com.example.wms.dto.check.CreateTaskRequest;
import com.example.wms.dto.check.ScanRequest;
import com.example.wms.entity.InventoryCheckDetail;
import com.example.wms.entity.InventoryCheckTask;
import com.example.wms.entity.InventoryStock;
import com.example.wms.entity.OutboundHistory;
import com.example.wms.repository.InventoryCheckDetailRepo;
import com.example.wms.repository.InventoryCheckTaskRepo;
import com.example.wms.repository.InventoryStockRepository;
import com.example.wms.repository.OutboundHistoryRepository;
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

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CheckServiceImpl 单元测试
 *
 * 测试覆盖：
 * - 创建盘点任务：正常创建、单号格式、system_qty快照、空筛选条件
 * - 扫码盘点：正常扫码、重复扫码校验、warehouseArea为空处理
 * - 差异调整：盘盈调整、盘亏调整、adjustQty目标值模式
 * - 完成任务：状态更新、时间戳设置
 * - 校验场景：任务/明细不存在
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("盘点服务测试")
public class CheckServiceImplTest {

    @Autowired
    private CheckServiceImpl checkService;

    @Autowired
    private InventoryCheckTaskRepo taskRepo;

    @Autowired
    private InventoryCheckDetailRepo detailRepo;

    @Autowired
    private InventoryStockRepository stockRepo;

    @Autowired
    private OutboundHistoryRepository outboundHistoryRepo;

    @Autowired
    private DataSource dataSource;

    // ==================== 测试数据 ====================

    @BeforeAll
    void initSchema() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema-h2.sql"));
        populator.setContinueOnError(true);
        populator.execute(dataSource);
    }

    @BeforeEach
    void setUp() {
        // 清理旧数据（因为 PER_CLASS 生命周期）
        outboundHistoryRepo.deleteAll();
        detailRepo.deleteAll();
        taskRepo.deleteAll();
        stockRepo.deleteAll();

        // 创建库存数据（FK 依赖）
        InventoryStock stock1 = new InventoryStock();
        stock1.setMaterialCode("MAT-ENG-001");
        stock1.setMaterialName("发动机支架");
        stock1.setSupplier("上海汽车零部件");
        stock1.setOnHandQty(100);
        stock1.setWarehouseArea("WA-AREA-01");
        stock1.setTransferStatus("不转包");
        stock1.setCreatedBy("system");
        stock1.setUpdatedBy("system");
        stock1.setCreatedAt(LocalDateTime.now());
        stock1.setUpdatedAt(LocalDateTime.now());
        stockRepo.save(stock1);

        InventoryStock stock2 = new InventoryStock();
        stock2.setMaterialCode("MAT-TOOL-001");
        stock2.setMaterialName("扭矩扳手");
        stock2.setSupplier("苏州精密器具");
        stock2.setOnHandQty(50);
        stock2.setWarehouseArea("WA-AREA-01");
        stock2.setTransferStatus("不转包");
        stock2.setCreatedBy("system");
        stock2.setUpdatedBy("system");
        stock2.setCreatedAt(LocalDateTime.now());
        stock2.setUpdatedAt(LocalDateTime.now());
        stockRepo.save(stock2);

        InventoryStock stock3 = new InventoryStock();
        stock3.setMaterialCode("MAT-ELE-001");
        stock3.setMaterialName("控制器模块");
        stock3.setSupplier("宁波电子模组");
        stock3.setOnHandQty(30);
        stock3.setWarehouseArea("WA-AREA-02");
        stock3.setTransferStatus("不转包");
        stock3.setCreatedBy("system");
        stock3.setUpdatedBy("system");
        stock3.setCreatedAt(LocalDateTime.now());
        stock3.setUpdatedAt(LocalDateTime.now());
        stockRepo.save(stock3);
    }

    // ==================== 1. 创建盘点任务 ====================

    @Nested
    @DisplayName("创建盘点任务")
    class CreateTaskTests {

        @Test
        @DisplayName("正常创建：按库区筛选，创建任务+明细")
        void shouldCreateTaskWithAreaFilter() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("库区1月度盘点");
            request.setCheckType("明盘");
            request.setWarehouseArea("WA-AREA-01");
            request.setMaterialCode(null);

            CheckTaskDTO result = checkService.createTask(request, "admin");

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("库区1月度盘点", result.getTaskName());
            assertEquals("明盘", result.getCheckType());
            assertEquals("进行中", result.getStatus());
            assertEquals("admin", result.getCreatedBy());
            assertEquals("WA-AREA-01", result.getWarehouseArea());

            // 验证明细数量（库区1有2条库存）
            assertEquals(2, result.getDetailCount());
            assertEquals(0, result.getCheckedCount());
        }

        @Test
        @DisplayName("正常创建：按物料筛选，创建任务+明细")
        void shouldCreateTaskWithMaterialFilter() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("指定物料盘点");
            request.setCheckType("明盘");
            request.setWarehouseArea(null);
            request.setMaterialCode("MAT-ENG-001");

            CheckTaskDTO result = checkService.createTask(request, "admin");

            assertNotNull(result);
            assertEquals(1, result.getDetailCount());
        }

        @Test
        @DisplayName("正常创建：同时按库区+物料筛选")
        void shouldCreateTaskWithBothFilters() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("精确盘点");
            request.setCheckType("明盘");
            request.setWarehouseArea("WA-AREA-01");
            request.setMaterialCode("MAT-ENG-001");

            CheckTaskDTO result = checkService.createTask(request, "operator");

            assertNotNull(result);
            assertEquals(1, result.getDetailCount());
        }

        @Test
        @DisplayName("空筛选条件：查询全量库存")
        void shouldCreateTaskWithNoFilters() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("全库盘点");
            request.setCheckType("明盘");
            request.setWarehouseArea(null);
            request.setMaterialCode(null);

            CheckTaskDTO result = checkService.createTask(request, "admin");

            assertNotNull(result);
            assertEquals(3, result.getDetailCount()); // 3条库存记录
        }

        @Test
        @DisplayName("创建时 system_qty 正确快照库存数量")
        void shouldSnapshotSystemQty() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("快照测试");
            request.setCheckType("明盘");
            request.setWarehouseArea("WA-AREA-01");
            request.setMaterialCode(null);

            CheckTaskDTO result = checkService.createTask(request, "admin");

            List<CheckDetailDTO> details = checkService.getTaskDetails(result.getId());
            assertEquals(2, details.size());

            // 找到 MAT-ENG-001 的明细，验证 system_qty 快照自 stock.on_hand_qty=100
            CheckDetailDTO engDetail = details.stream()
                    .filter(d -> "MAT-ENG-001".equals(d.getMaterialCode()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(100, engDetail.getSystemQty());

            // 找到 MAT-TOOL-001 的明细，验证 system_qty 快照自 stock.on_hand_qty=50
            CheckDetailDTO toolDetail = details.stream()
                    .filter(d -> "MAT-TOOL-001".equals(d.getMaterialCode()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(50, toolDetail.getSystemQty());
        }

        @Test
        @DisplayName("单号格式：PD-yyyyMMdd-xxx")
        void shouldGenerateCorrectTaskNoFormat() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("单号格式测试");
            request.setCheckType("明盘");

            CheckTaskDTO result = checkService.createTask(request, "admin");

            String taskNo = result.getTaskNo();
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            assertTrue(taskNo.startsWith("PD-" + today + "-"),
                    "单号应以 PD-yyyyMMdd- 开头，实际: " + taskNo);
            assertTrue(taskNo.matches("PD-\\d{8}-\\d{3}"),
                    "单号格式应为 PD-yyyyMMdd-xxx，实际: " + taskNo);
        }

        @Test
        @DisplayName("连续创建：序号递增")
        void shouldGenerateSequentialTaskNo() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("序号测试1");
            request.setCheckType("明盘");

            CheckTaskDTO result1 = checkService.createTask(request, "admin");

            request.setTaskName("序号测试2");
            CheckTaskDTO result2 = checkService.createTask(request, "admin");

            request.setTaskName("序号测试3");
            CheckTaskDTO result3 = checkService.createTask(request, "admin");

            // 提取序号
            String no1 = result1.getTaskNo();
            String no2 = result2.getTaskNo();
            String no3 = result3.getTaskNo();

            int seq1 = Integer.parseInt(no1.substring(no1.lastIndexOf('-') + 1));
            int seq2 = Integer.parseInt(no2.substring(no2.lastIndexOf('-') + 1));
            int seq3 = Integer.parseInt(no3.substring(no3.lastIndexOf('-') + 1));

            assertEquals(1, seq2 - seq1, "序号应递增");
            assertEquals(1, seq3 - seq2, "序号应递增");
        }

        @Test
        @DisplayName("盲盘模式创建")
        void shouldCreateBlindCheckTask() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("盲盘测试");
            request.setCheckType("盲盘");
            request.setWarehouseArea("WA-AREA-02");

            CheckTaskDTO result = checkService.createTask(request, "admin");

            assertNotNull(result);
            assertEquals("盲盘", result.getCheckType());
            assertEquals(1, result.getDetailCount());
        }
    }

    // ==================== 2. 扫码盘点 ====================

    @Nested
    @DisplayName("扫码盘点")
    class ScanCheckTests {

        private InventoryCheckTask task;
        private InventoryCheckDetail detail;

        @BeforeEach
        void createTaskAndDetail() {
            // 创建任务
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("扫码测试任务");
            request.setCheckType("明盘");
            request.setWarehouseArea("WA-AREA-01");
            CheckTaskDTO taskDTO = checkService.createTask(request, "admin");
            task = taskRepo.findById(taskDTO.getId()).orElseThrow();

            List<CheckDetailDTO> details = checkService.getTaskDetails(task.getId());
            detail = detailRepo.findById(details.get(0).getId()).orElseThrow();
        }

        @Test
        @DisplayName("正常扫码：更新 actual_qty，计算 diff_qty")
        void shouldScanAndCalculateDiff() {
            ScanRequest scanReq = new ScanRequest();
            scanReq.setTaskId(task.getId());
            scanReq.setMaterialCode(detail.getMaterialCode());
            scanReq.setWarehouseArea(detail.getWarehouseArea());
            scanReq.setActualQty(95);
            scanReq.setCheckedBy("scanner");

            CheckProgressDTO progress = checkService.scanCheck(scanReq);

            // 验证进度返回
            assertNotNull(progress);
            assertEquals(task.getId(), progress.getTaskId());
            assertEquals(2, progress.getTotal());
            assertEquals(1, progress.getChecked());
            assertEquals(50, progress.getProgressPercent()); // 1/2 = 50%
            assertEquals(1, progress.getDiffCount()); // 有差异

            // 验证明细更新
            InventoryCheckDetail updated = detailRepo.findById(detail.getId()).orElseThrow();
            assertEquals(95, updated.getActualQty());
            assertEquals(95 - updated.getSystemQty(), updated.getDiffQty());
            assertEquals("已盘", updated.getStatus());
            assertEquals("scanner", updated.getCheckedBy());
            assertNotNull(updated.getCheckedAt());
        }

        @Test
        @DisplayName("扫码后 diff_qty = actual_qty - system_qty")
        void shouldCalculateDiffCorrectly() {
            // 实际清点数 = 80（比系统100少20）
            ScanRequest scanReq = new ScanRequest();
            scanReq.setTaskId(task.getId());
            scanReq.setMaterialCode(detail.getMaterialCode());
            scanReq.setWarehouseArea(detail.getWarehouseArea());
            scanReq.setActualQty(80);
            scanReq.setCheckedBy("checker");

            checkService.scanCheck(scanReq);

            InventoryCheckDetail updated = detailRepo.findById(detail.getId()).orElseThrow();
            assertEquals(80, updated.getActualQty());
            assertEquals(80 - 100, updated.getDiffQty()); // -20
        }

        @Test
        @DisplayName("重复扫码：状态已变'已盘'，再次扫码应抛异常")
        void shouldRejectDuplicateScan() {
            ScanRequest scanReq = new ScanRequest();
            scanReq.setTaskId(task.getId());
            scanReq.setMaterialCode(detail.getMaterialCode());
            scanReq.setWarehouseArea(detail.getWarehouseArea());
            scanReq.setActualQty(90);
            scanReq.setCheckedBy("scanner");

            // 第一次扫码
            checkService.scanCheck(scanReq);

            // 第二次扫码应抛异常
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> checkService.scanCheck(scanReq),
                    "重复扫码应抛异常");
            assertEquals("该物料已盘点，请勿重复扫码", ex.getMessage());
        }

        @Test
        @DisplayName("扫码时 warehouseArea 为空：按物料编码匹配")
        void shouldScanWithoutWarehouseArea() {
            ScanRequest scanReq = new ScanRequest();
            scanReq.setTaskId(task.getId());
            scanReq.setMaterialCode(detail.getMaterialCode());
            scanReq.setWarehouseArea(null); // 空
            scanReq.setActualQty(88);
            scanReq.setCheckedBy("scanner");

            CheckProgressDTO progress = checkService.scanCheck(scanReq);

            assertNotNull(progress);
            assertEquals(1, progress.getChecked());

            InventoryCheckDetail updated = detailRepo.findById(detail.getId()).orElseThrow();
            assertEquals(88, updated.getActualQty());
        }

        @Test
        @DisplayName("扫码不存在的明细应抛异常")
        void shouldThrowForNonExistentDetail() {
            ScanRequest scanReq = new ScanRequest();
            scanReq.setTaskId(task.getId());
            scanReq.setMaterialCode("NON-EXISTENT");
            scanReq.setWarehouseArea("WA-AREA-01");
            scanReq.setActualQty(10);
            scanReq.setCheckedBy("scanner");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> checkService.scanCheck(scanReq),
                    "不存在的明细应抛异常");
            assertEquals("未找到盘点明细", ex.getMessage());
        }

        @Test
        @DisplayName("扫码后状态为'已盘'")
        void shouldSetStatusToChecked() {
            ScanRequest scanReq = new ScanRequest();
            scanReq.setTaskId(task.getId());
            scanReq.setMaterialCode(detail.getMaterialCode());
            scanReq.setWarehouseArea(detail.getWarehouseArea());
            scanReq.setActualQty(100);
            scanReq.setCheckedBy("checker");

            checkService.scanCheck(scanReq);

            InventoryCheckDetail updated = detailRepo.findById(detail.getId()).orElseThrow();
            assertEquals("已盘", updated.getStatus());
        }
    }

    // ==================== 3. 差异调整 ====================

    @Nested
    @DisplayName("差异调整")
    class AdjustDetailTests {

        private InventoryCheckTask task;
        private InventoryCheckDetail detail;
        private InventoryStock stock;

        @BeforeEach
        void createTaskAndScan() {
            // 创建任务（只有一条库存 WA-AREA-01 / MAT-ENG-001, on_hand_qty=100）
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("调整测试任务");
            request.setCheckType("明盘");
            request.setWarehouseArea("WA-AREA-01");
            request.setMaterialCode("MAT-ENG-001");
            CheckTaskDTO taskDTO = checkService.createTask(request, "admin");
            task = taskRepo.findById(taskDTO.getId()).orElseThrow();

            List<CheckDetailDTO> details = checkService.getTaskDetails(task.getId());
            detail = detailRepo.findById(details.get(0).getId()).orElseThrow();
            stock = stockRepo.findByMaterialCodeAndSupplierAndWarehouseArea(
                    "MAT-ENG-001", "上海汽车零部件", "WA-AREA-01").orElseThrow();

            // 扫码：实际60，系统100，亏40
            ScanRequest scanReq = new ScanRequest();
            scanReq.setTaskId(task.getId());
            scanReq.setMaterialCode(detail.getMaterialCode());
            scanReq.setWarehouseArea(detail.getWarehouseArea());
            scanReq.setActualQty(60);
            scanReq.setCheckedBy("checker");
            checkService.scanCheck(scanReq);
        }

        @Test
        @DisplayName("盘亏调整（diff<0）：库存减少，写 outbound_history（亏）")
        void shouldAdjustLoss() {
            // diff = 60 - 100 = -40，调整后目标数量80（介于实际和系统之间）
            int adjustQty = 80;

            checkService.adjustDetail(detail.getId(), adjustQty, "admin");

            // 验证库存已更新为目标值
            InventoryStock updatedStock = stockRepo.findById(stock.getId()).orElseThrow();
            assertEquals(80, updatedStock.getOnHandQty());

            // 验证 outbound_history（亏）记录
            List<OutboundHistory> histories = outboundHistoryRepo.findAll();
            assertEquals(1, histories.size());

            OutboundHistory history = histories.get(0);
            assertEquals("PD-" + task.getTaskNo() + "-亏", history.getDocNo());
            assertEquals(40, history.getIssueQty()); // 盘亏量 Math.abs(-40)
            assertEquals("MAT-ENG-001", history.getMaterialCode());
            assertEquals("admin", history.getIssuedBy());

            // 验证明细状态已变为"已调整"
            InventoryCheckDetail updatedDetail = detailRepo.findById(detail.getId()).orElseThrow();
            assertEquals("已调整", updatedDetail.getStatus());
        }

        @Test
        @DisplayName("盘盈调整（diff>0）：库存增加，写 outbound_history（盈）")
        void shouldAdjustGain() {
            // 先更新扫码为实际80，系统100，亏20
            InventoryCheckDetail detailToGain = detailRepo.findById(detail.getId()).orElseThrow();
            detailToGain.setActualQty(120); // 实际比系统多20
            detailToGain.setDiffQty(20);
            detailToGain.setStatus("已盘");
            detailToGain.setCheckedBy("checker");
            detailToGain.setCheckedAt(LocalDateTime.now());
            detailRepo.save(detailToGain);

            // 调整后目标数量150
            int adjustQty = 150;

            checkService.adjustDetail(detail.getId(), adjustQty, "manager");

            // 验证库存已更新为目标值
            InventoryStock updatedStock = stockRepo.findById(stock.getId()).orElseThrow();
            assertEquals(150, updatedStock.getOnHandQty());

            // 验证 outbound_history（盈）记录
            List<OutboundHistory> histories = outboundHistoryRepo.findAll();
            assertEquals(1, histories.size());

            OutboundHistory history = histories.get(0);
            assertEquals("PD-" + task.getTaskNo() + "-盈", history.getDocNo());
            assertEquals(20, history.getIssueQty()); // 盘盈量
            assertEquals("MAT-ENG-001", history.getMaterialCode());
            assertEquals("manager", history.getIssuedBy());

            // 验证明细状态
            InventoryCheckDetail updatedDetail = detailRepo.findById(detail.getId()).orElseThrow();
            assertEquals("已调整", updatedDetail.getStatus());
        }

        @Test
        @DisplayName("adjustQty 是目标值模式：直接设为目标数量")
        void shouldSetTargetQtyDirectly() {
            // 调整到 200（无论 diff 是正还是负）
            checkService.adjustDetail(detail.getId(), 200, "admin");

            InventoryStock updatedStock = stockRepo.findById(stock.getId()).orElseThrow();
            assertEquals(200, updatedStock.getOnHandQty()); // 直接是200，不是增量
        }

        @Test
        @DisplayName("调整后 detail.status 变为'已调整'")
        void shouldSetStatusToAdjusted() {
            checkService.adjustDetail(detail.getId(), 80, "admin");

            InventoryCheckDetail updatedDetail = detailRepo.findById(detail.getId()).orElseThrow();
            assertEquals("已调整", updatedDetail.getStatus());
        }

        @Test
        @DisplayName("无差异（diff=0）应抛异常")
        void shouldRejectAdjustNoDiff() {
            // 设置 diff=0
            InventoryCheckDetail noDiff = detailRepo.findById(detail.getId()).orElseThrow();
            noDiff.setActualQty(100); // = system_qty
            noDiff.setDiffQty(0);
            detailRepo.save(noDiff);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> checkService.adjustDetail(detail.getId(), 100, "admin"),
                    "无差异应抛异常");
            assertEquals("无差异，无需调整", ex.getMessage());
        }

        @Test
        @DisplayName("状态不是'已盘'应抛异常")
        void shouldRejectAdjustNotChecked() {
            // 状态还是"待盘"
            InventoryCheckDetail notChecked = detailRepo.findById(detail.getId()).orElseThrow();
            notChecked.setStatus("待盘");
            detailRepo.save(notChecked);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> checkService.adjustDetail(detail.getId(), 80, "admin"),
                    "未盘点应抛异常");
            assertEquals("只能调整已盘的明细", ex.getMessage());
        }

        @Test
        @DisplayName("调整不存在的明细应抛异常")
        void shouldThrowForNonExistentDetail() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> checkService.adjustDetail(99999L, 80, "admin"),
                    "不存在的明细应抛异常");
            assertEquals("明细不存在", ex.getMessage());
        }

        @Test
        @DisplayName("库存记录不存在应抛异常")
        void shouldThrowWhenStockNotFound() {
            // 先删掉库存记录
            stockRepo.delete(stock);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> checkService.adjustDetail(detail.getId(), 80, "admin"),
                    "库存记录不存在应抛异常");
            assertEquals("库存记录不存在", ex.getMessage());
        }
    }

    // ==================== 4. 完成任务 ====================

    @Nested
    @DisplayName("完成任务")
    class CompleteTaskTests {

        @Test
        @DisplayName("完成任务：更新状态为'已完成'，设置 completed_at")
        void shouldCompleteTask() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("完成测试任务");
            request.setCheckType("明盘");
            CheckTaskDTO taskDTO = checkService.createTask(request, "admin");
            InventoryCheckTask task = taskRepo.findById(taskDTO.getId()).orElseThrow();

            assertEquals("进行中", task.getStatus());
            assertNull(task.getCompletedAt());

            checkService.completeTask(task.getId());

            InventoryCheckTask completed = taskRepo.findById(task.getId()).orElseThrow();
            assertEquals("已完成", completed.getStatus());
            assertNotNull(completed.getCompletedAt());
        }

        @Test
        @DisplayName("完成任务：已完成状态再次调用应抛异常")
        void shouldRejectCompleteAlreadyCompleted() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("双重完成测试");
            request.setCheckType("明盘");
            CheckTaskDTO taskDTO = checkService.createTask(request, "admin");
            InventoryCheckTask task = taskRepo.findById(taskDTO.getId()).orElseThrow();

            checkService.completeTask(task.getId());

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> checkService.completeTask(task.getId()),
                    "已完成任务再次完成应抛异常");
            assertEquals("只能完成进行中的任务", ex.getMessage());
        }

        @Test
        @DisplayName("完成任务：不存在应抛异常")
        void shouldThrowForNonExistentTask() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> checkService.completeTask(99999L),
                    "不存在的任务应抛异常");
            assertEquals("任务不存在", ex.getMessage());
        }
    }

    // ==================== 5. 校验场景 ====================

    @Nested
    @DisplayName("校验场景")
    class ValidationTests {

        @Test
        @DisplayName("查询不存在的任务详情应返回空列表")
        void shouldReturnEmptyListForNonExistentTask() {
            List<CheckDetailDTO> details = checkService.getTaskDetails(99999L);
            assertNotNull(details);
            assertTrue(details.isEmpty());
        }

        @Test
        @DisplayName("查询存在的任务详情应返回明细列表")
        void shouldReturnDetailsForExistingTask() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("详情测试");
            request.setCheckType("明盘");
            request.setWarehouseArea("WA-AREA-01");
            CheckTaskDTO result = checkService.createTask(request, "admin");

            List<CheckDetailDTO> details = checkService.getTaskDetails(result.getId());

            assertNotNull(details);
            assertEquals(2, details.size());
        }

        @Test
        @DisplayName("getDetail：不存在的明细应抛异常")
        void shouldThrowForNonExistentDetail() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> checkService.getDetail(99999L),
                    "不存在的明细应抛异常");
            assertEquals("明细不存在", ex.getMessage());
        }

        @Test
        @DisplayName("扫码：不存在的任务应抛异常")
        void shouldThrowForNonExistentTaskOnScan() {
            ScanRequest scanReq = new ScanRequest();
            scanReq.setTaskId(99999L);
            scanReq.setMaterialCode("MAT-ENG-001");
            scanReq.setActualQty(10);
            scanReq.setCheckedBy("scanner");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> checkService.scanCheck(scanReq),
                    "不存在的任务应抛异常");
            assertEquals("未找到盘点明细", ex.getMessage()); // 先命中明细查询
        }

        @Test
        @DisplayName("listActiveTasks：返回进行中的任务列表")
        void shouldListActiveTasks() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("进行中任务");
            request.setCheckType("明盘");
            checkService.createTask(request, "admin");

            List<CheckProgressDTO> activeTasks = checkService.listActiveTasks();

            assertNotNull(activeTasks);
            assertFalse(activeTasks.isEmpty());
            assertTrue(activeTasks.stream().anyMatch(t -> "进行中任务".equals(t.getTaskName())));
        }

        @Test
        @DisplayName("listTasks：返回所有任务列表")
        void shouldListAllTasks() {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTaskName("列表测试");
            request.setCheckType("明盘");
            checkService.createTask(request, "admin");

            List<CheckTaskDTO> tasks = checkService.listTasks();

            assertNotNull(tasks);
            assertFalse(tasks.isEmpty());
        }
    }

    // ==================== 端到端场景 ====================

    @Nested
    @DisplayName("端到端场景")
    class E2ETests {

        @Test
        @DisplayName("完整盘点流程：创建 -> 扫码 -> 调整 -> 完成")
        void shouldCompleteFullCheckFlow() {
            // 1. 创建任务（全量）
            CreateTaskRequest createReq = new CreateTaskRequest();
            createReq.setTaskName("完整流程测试");
            createReq.setCheckType("明盘");
            CheckTaskDTO taskDTO = checkService.createTask(createReq, "admin");
            Long taskId = taskDTO.getId();

            // 2. 获取明细，开始扫码
            List<CheckDetailDTO> details = checkService.getTaskDetails(taskId);
            assertEquals(3, details.size());

            // 扫码 MAT-ENG-001（系统100），实际90，亏10
            CheckDetailDTO d1 = details.stream()
                    .filter(d -> "MAT-ENG-001".equals(d.getMaterialCode()))
                    .findFirst().orElseThrow();

            ScanRequest scan1 = new ScanRequest();
            scan1.setTaskId(taskId);
            scan1.setMaterialCode(d1.getMaterialCode());
            scan1.setWarehouseArea(d1.getWarehouseArea());
            scan1.setActualQty(90);
            scan1.setCheckedBy("checker");
            CheckProgressDTO progress1 = checkService.scanCheck(scan1);

            assertEquals(1, progress1.getChecked());
            assertEquals(1, progress1.getDiffCount());

            // 3. 调整差异
            checkService.adjustDetail(d1.getId(), 90, "admin");

            InventoryCheckDetail adjusted = detailRepo.findById(d1.getId()).orElseThrow();
            assertEquals("已调整", adjusted.getStatus());

            // 验证库存已更新
            InventoryStock stock = stockRepo.findByMaterialCodeAndSupplierAndWarehouseArea(
                    "MAT-ENG-001", "上海汽车零部件", "WA-AREA-01").orElseThrow();
            assertEquals(90, stock.getOnHandQty());

            // 4. 完成任务
            checkService.completeTask(taskId);

            InventoryCheckTask completed = taskRepo.findById(taskId).orElseThrow();
            assertEquals("已完成", completed.getStatus());
            assertNotNull(completed.getCompletedAt());
        }
    }
}
