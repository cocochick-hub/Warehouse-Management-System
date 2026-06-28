package com.example.wms.service.impl;

import com.example.wms.dto.transfer.TransferRequest;
import com.example.wms.dto.transfer.TransferResultDTO;
import com.example.wms.entity.InboundKanbanLabel;
import com.example.wms.entity.PackageTransfer;
import com.example.wms.repository.InboundKanbanLabelRepository;
import com.example.wms.repository.PackageTransferRepository;
import com.example.wms.service.TransferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 转包服务实现
 *
 * 支持两种转包模式：
 * 1. 向下转包（拆包）：源看板 → 新建目标看板，数量拆分
 * 2. 向上转包（合包）：源看板 → 已有目标看板，数量累加
 *
 * 约束：
 * - 只能对"已入库"且未封存的看板操作
 * - 转移数量不得超过源看板可用数量
 * - 支持全量转移（源看板数量归零）
 */
@Service
public class TransferServiceImpl implements TransferService {

    private final InboundKanbanLabelRepository kanbanLabelRepository;
    private final PackageTransferRepository transferRepository;

    public TransferServiceImpl(InboundKanbanLabelRepository kanbanLabelRepository,
                               PackageTransferRepository transferRepository) {
        this.kanbanLabelRepository = kanbanLabelRepository;
        this.transferRepository = transferRepository;
    }

    @Override
    @Transactional
    public TransferResultDTO executeTransfer(TransferRequest request, String operator) {
        // 1. 查询源看板
        InboundKanbanLabel source = kanbanLabelRepository.findByKanbanNo(request.getSourceKanbanNo())
                .orElseThrow(() -> new EntityNotFoundException("看板号不存在: " + request.getSourceKanbanNo()));

        // 2. 基础校验
        if (!"已入库".equals(source.getLabelStatus())) {
            throw new IllegalStateException("只有已入库的看板才能转包，当前状态: " + source.getLabelStatus());
        }
        if (Boolean.TRUE.equals(source.getSealed())) {
            throw new IllegalStateException("该看板已封存，无法转包，请先解封");
        }

        int sourceQty = source.getLabelQty();
        int transferQty = request.getTransferQty();

        if (transferQty <= 0) {
            throw new IllegalArgumentException("转移数量必须大于0");
        }
        if (transferQty > sourceQty) {
            throw new IllegalArgumentException(
                    "转移数量(" + transferQty + ")超过源看板可用数量(" + sourceQty + ")");
        }

        // 3. 确定目标看板：是新建还是汇入已有
        String targetKanbanNo = request.getTargetKanbanNo();
        InboundKanbanLabel target;
        int targetQtyBefore;

        if (targetKanbanNo != null && !targetKanbanNo.trim().isEmpty()) {
            // 用户指定了目标看板号 → 可能是合包（向上转包）
            target = kanbanLabelRepository.findByKanbanNo(targetKanbanNo).orElse(null);

            if (target != null) {
                // === 向上转包：汇入已有看板 ===
                if (!source.getMaterialCode().equals(target.getMaterialCode())) {
                    throw new IllegalArgumentException("目标看板物料号(" + target.getMaterialCode()
                            + ")与源看板(" + source.getMaterialCode() + ")不一致，不能合包");
                }
                if (!"已入库".equals(target.getLabelStatus())) {
                    throw new IllegalStateException("目标看板状态不是已入库，不能汇入");
                }
                if (Boolean.TRUE.equals(target.getSealed())) {
                    throw new IllegalStateException("目标看板已封存，不能汇入");
                }
                targetQtyBefore = target.getLabelQty();
                target.setLabelQty(targetQtyBefore + transferQty);
                target.setUpdatedBy(operator);
                target.setUpdatedAt(LocalDateTime.now());
                target.setTransferStatus("转入");
            } else {
                // === 向下转包：指定看板号不存在 → 新建 ===
                target = createTargetKanban(source, targetKanbanNo, transferQty, operator);
                targetQtyBefore = 0;
            }
        } else {
            // === 向下转包：自动生成看板号 → 新建 ===
            targetKanbanNo = generateTargetKanbanNo(source.getKanbanNo());
            target = createTargetKanban(source, targetKanbanNo, transferQty, operator);
            targetQtyBefore = 0;
        }

        kanbanLabelRepository.save(target);

        // 4. 更新源看板数量
        int sourceQtyAfter = sourceQty - transferQty;
        source.setLabelQty(sourceQtyAfter);
        source.setTransferStatus(sourceQtyAfter == 0 ? "已转包" : "转包");
        source.setUpdatedBy(operator);
        source.setUpdatedAt(LocalDateTime.now());
        kanbanLabelRepository.save(source);

        // 5. 记录转包历史
        PackageTransfer record = new PackageTransfer();
        record.setSourceKanbanNo(source.getKanbanNo());
        record.setTargetKanbanNo(targetKanbanNo);
        record.setTransferQty(transferQty);
        record.setSourceQtyBefore(sourceQty);
        record.setSourceQtyAfter(sourceQtyAfter);
        record.setMaterialCode(source.getMaterialCode());
        record.setMaterialName(source.getMaterialName());
        record.setSupplierName(source.getSupplierName());
        record.setOperator(operator);
        record.setCreatedAt(LocalDateTime.now());
        transferRepository.save(record);

        // 6. 返回结果
        return new TransferResultDTO(
                source.getKanbanNo(),
                sourceQtyAfter,
                targetKanbanNo,
                target.getLabelQty(),
                source.getMaterialCode(),
                source.getMaterialName(),
                source.getSupplierName(),
                source.getWarehouseArea(),
                LocalDateTime.now()
        );
    }

    /** 创建目标看板，从源看板复制物料信息 */
    private InboundKanbanLabel createTargetKanban(InboundKanbanLabel source, String kanbanNo,
                                                   int qty, String operator) {
        InboundKanbanLabel target = new InboundKanbanLabel();
        target.setDocNo(source.getDocNo());
        target.setKanbanNo(kanbanNo);
        target.setQrPayload("WMS-INBOUND|" + kanbanNo);
        target.setMaterialCode(source.getMaterialCode());
        target.setMaterialName(source.getMaterialName());
        target.setSupplierCode(source.getSupplierCode());
        target.setSupplierName(source.getSupplierName());
        target.setPackageModel(source.getPackageModel());
        target.setWarehouseArea(source.getWarehouseArea());
        target.setLabelQty(qty);
        target.setLabelStatus("已入库");
        target.setInboundOrderId(source.getInboundOrderId());
        target.setInboundOrderDetailId(source.getInboundOrderDetailId());
        target.setPackageSeq(source.getPackageSeq());
        target.setPackageTotal(source.getPackageTotal());
        target.setTransferStatus("转入");
        target.setCreatedBy(operator);
        target.setUpdatedBy(operator);
        target.setCreatedAt(LocalDateTime.now());
        target.setUpdatedAt(LocalDateTime.now());
        return target;
    }

    /** 生成目标看板号: T-{date}-{srcShort}-{seq} */
    private String generateTargetKanbanNo(String sourceKanbanNo) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String shortSrc = sourceKanbanNo.length() > 20
                ? sourceKanbanNo.substring(sourceKanbanNo.length() - 8)
                : sourceKanbanNo.replaceAll("[^a-zA-Z0-9]", "-");
        long seq = System.currentTimeMillis() % 100000;
        return "T-" + date + "-" + shortSrc + "-" + seq;
    }
}
