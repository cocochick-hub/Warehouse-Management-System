package com.example.wms.service.impl;

import com.example.wms.dto.seal.SealBatchResultDTO;
import com.example.wms.dto.seal.SealLabelDTO;
import com.example.wms.dto.seal.SealLabelRequest;
import com.example.wms.entity.InboundKanbanLabel;
import com.example.wms.entity.OutboundHistory;
import com.example.wms.repository.InboundKanbanLabelRepository;
import com.example.wms.repository.OutboundHistoryRepository;
import com.example.wms.service.SealService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SealServiceImpl implements SealService {

    private static final String STATUS_RECEIVED = "已入库";

    private final InboundKanbanLabelRepository inboundKanbanLabelRepository;
    private final OutboundHistoryRepository outboundHistoryRepository;

    public SealServiceImpl(InboundKanbanLabelRepository inboundKanbanLabelRepository,
                           OutboundHistoryRepository outboundHistoryRepository) {
        this.inboundKanbanLabelRepository = inboundKanbanLabelRepository;
        this.outboundHistoryRepository = outboundHistoryRepository;
    }

    @Override
    public SealLabelDTO getLabelByKanbanNo(String kanbanNo) {
        InboundKanbanLabel label = inboundKanbanLabelRepository.findByKanbanNo(kanbanNo)
                .orElseThrow(() -> new EntityNotFoundException("看板不存在: " + kanbanNo));
        return toDTO(label);
    }

    @Override
    public List<SealLabelDTO> listSealedLabels(String materialCode, String supplierName) {
        // 查询所有已入库的看板，前端可按封存状态过滤
        List<InboundKanbanLabel> all = inboundKanbanLabelRepository.findAll();
        return all.stream()
                .filter(label -> STATUS_RECEIVED.equals(label.getLabelStatus()))
                .filter(label -> Boolean.TRUE.equals(label.getSealed()))
                .filter(label -> materialCode == null || materialCode.trim().isEmpty()
                        || label.getMaterialCode().contains(materialCode.trim()))
                .filter(label -> supplierName == null || supplierName.trim().isEmpty()
                        || label.getSupplierName().contains(supplierName.trim()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SealLabelDTO toggleSealSingle(String kanbanNo, String action, String operator) {
        InboundKanbanLabel label = inboundKanbanLabelRepository.findByKanbanNo(kanbanNo)
                .orElseThrow(() -> new EntityNotFoundException("看板不存在: " + kanbanNo));

        if (!STATUS_RECEIVED.equals(label.getLabelStatus())) {
            throw new IllegalStateException("只有已入库的看板才能封存/解封，当前状态: " + label.getLabelStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        boolean seal = "seal".equals(action);

        if (seal) {
            if (Boolean.TRUE.equals(label.getSealed())) {
                throw new IllegalStateException("该看板已被封存，无需重复操作");
            }
            label.setSealed(true);
            label.setSealedAt(now);
            label.setSealedBy(operator);
        } else {
            if (!Boolean.TRUE.equals(label.getSealed())) {
                throw new IllegalStateException("该看板未被封存，无需解封");
            }
            label.setSealed(false);
            label.setSealedAt(null);
            label.setSealedBy(null);
        }

        label.setUpdatedBy(operator);
        label.setUpdatedAt(now);
        inboundKanbanLabelRepository.save(label);

        return toDTO(label);
    }

    @Override
    @Transactional
    public SealBatchResultDTO toggleSealBatch(SealLabelRequest request, String operator) {
        List<String> kanbanNos = request.getKanbanNos();
        if (kanbanNos == null || kanbanNos.isEmpty()) {
            throw new IllegalArgumentException("看板号列表不能为空");
        }

        String action = request.getAction();
        if (action == null || (!"seal".equals(action) && !"unseal".equals(action))) {
            throw new IllegalArgumentException("操作类型无效，请指定 seal 或 unseal");
        }

        int successCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();

        for (String kanbanNo : kanbanNos) {
            try {
                toggleSealSingle(kanbanNo.trim(), action, operator);
                successCount++;
            } catch (Exception e) {
                failCount++;
                failReasons.add("看板 " + kanbanNo + ": " + e.getMessage());
            }
        }

        return new SealBatchResultDTO(successCount, failCount, failReasons);
    }

    private int calculateAvailableQty(InboundKanbanLabel label) {
        List<OutboundHistory> consumed = outboundHistoryRepository
                .findBySourceDetailId(label.getInboundOrderDetailId());
        int consumedQty = consumed.stream()
                .filter(h -> !"已退库".equals(h.getStatus()))
                .mapToInt(h -> {
                    Integer qty = h.getIssueQty();
                    return qty == null ? 0 : qty;
                })
                .sum();
        int labelQty = label.getLabelQty() == null ? 0 : label.getLabelQty();
        return Math.max(labelQty - consumedQty, 0);
    }

    private SealLabelDTO toDTO(InboundKanbanLabel label) {
        return new SealLabelDTO(
                label.getId(),
                label.getKanbanNo(),
                label.getDocNo(),
                label.getMaterialCode(),
                label.getMaterialName(),
                label.getSupplierName(),
                label.getLabelQty(),
                label.getWarehouseArea(),
                label.getLabelStatus(),
                label.getSealed(),
                label.getSealedAt(),
                label.getSealedBy(),
                calculateAvailableQty(label)
        );
    }
}
