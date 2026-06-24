package com.example.wms.service.impl;

import com.example.wms.dto.demand.DemandBatchDTO;
import com.example.wms.dto.demand.DemandCreateRequest;
import com.example.wms.dto.demand.DemandDetailDTO;
import com.example.wms.entity.DemandBatch;
import com.example.wms.entity.DemandDetail;
import com.example.wms.repository.DemandBatchRepository;
import com.example.wms.repository.DemandDetailRepository;
import com.example.wms.service.DemandService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 物料需求管理 Service 实现
 */
@Service
public class DemandServiceImpl implements DemandService {

    private final DemandBatchRepository batchRepo;
    private final DemandDetailRepository detailRepo;

    public DemandServiceImpl(DemandBatchRepository batchRepo,
                             DemandDetailRepository detailRepo) {
        this.batchRepo = batchRepo;
        this.detailRepo = detailRepo;
    }

    @Override
    @Transactional
    public DemandBatchDTO createDemand(DemandCreateRequest request, String operator) {
        List<DemandCreateRequest.DemandItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("需求明细不能为空");
        }

        // 生成批次号
        String batchNo = "BTH-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        int totalQty = 0;
        LocalDateTime now = LocalDateTime.now();

        List<DemandDetail> details = new ArrayList<>();
        for (DemandCreateRequest.DemandItem item : items) {
            if (item.getDemandQty() == null || item.getDemandQty() <= 0) {
                throw new IllegalArgumentException("物料 " + item.getMaterialCode() + " 的需求数量必须大于0");
            }
            DemandDetail detail = new DemandDetail();
            detail.setBatchNo(batchNo);
            detail.setMaterialCode(item.getMaterialCode().trim());
            detail.setMaterialName(nvl(item.getMaterialName(), "").trim());
            detail.setSupplierCode(nvl(item.getSupplierCode(), "").trim());
            detail.setSupplierName(nvl(item.getSupplierName(), "").trim());
            detail.setDemandQty(item.getDemandQty());
            detail.setFulfilledQty(0);
            detail.setDemandDate(item.getDemandDate());
            detail.setWarehouseArea(nvl(item.getWarehouseArea(), "默认库区"));
            detail.setStatus("待出库");
            detail.setRemark(nvl(item.getRemark(), null));
            detail.setCreatedAt(now);
            details.add(detail);
            totalQty += item.getDemandQty();
        }

        // 先保存批次
        DemandBatch batch = new DemandBatch();
        batch.setBatchNo(batchNo);
        batch.setItemCount(details.size());
        batch.setTotalQty(totalQty);
        batch.setImportType("MANUAL");
        batch.setCreatedBy(nvl(operator, "system"));
        batch.setCreatedAt(now);
        batchRepo.save(batch);

        // 回填 batchId
        for (DemandDetail detail : details) {
            detail.setBatchId(batch.getId());
        }
        detailRepo.saveAll(details);

        return toBatchDTO(batch);
    }

    @Override
    public Page<DemandDetailDTO> listDemands(Integer page, Integer size,
                                              String materialCode, String materialName,
                                              String supplier, String status) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        PageRequest pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<DemandDetail> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (notBlank(materialCode))
                predicates.add(cb.like(root.get("materialCode"), "%" + materialCode.trim() + "%"));
            if (notBlank(materialName))
                predicates.add(cb.like(root.get("materialName"), "%" + materialName.trim() + "%"));
            if (notBlank(supplier))
                predicates.add(cb.like(root.get("supplierName"), "%" + supplier.trim() + "%"));
            if (notBlank(status))
                predicates.add(cb.equal(root.get("status"), status.trim()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DemandDetail> result = detailRepo.findAll(spec, pageable);
        return result.map(this::toDetailDTO);
    }

    @Override
    public DemandBatchDTO getBatch(String batchNo) {
        return batchRepo.findByBatchNo(batchNo.trim())
                .map(this::toBatchDTO)
                .orElse(null);
    }

    // ==================== 私有转换方法 ====================

    private DemandBatchDTO toBatchDTO(DemandBatch b) {
        return new DemandBatchDTO(b.getId(), b.getBatchNo(), b.getItemCount(),
                b.getTotalQty(), b.getImportType(), b.getCreatedBy(), b.getCreatedAt());
    }

    private DemandDetailDTO toDetailDTO(DemandDetail d) {
        DemandDetailDTO dto = new DemandDetailDTO();
        dto.setId(d.getId());
        dto.setBatchId(d.getBatchId());
        dto.setBatchNo(d.getBatchNo());
        dto.setMaterialCode(d.getMaterialCode());
        dto.setMaterialName(d.getMaterialName());
        dto.setSupplierCode(d.getSupplierCode());
        dto.setSupplierName(d.getSupplierName());
        dto.setDemandQty(d.getDemandQty());
        dto.setFulfilledQty(d.getFulfilledQty() != null ? d.getFulfilledQty() : 0);
        dto.setDemandDate(d.getDemandDate());
        dto.setWarehouseArea(d.getWarehouseArea());
        dto.setStatus(d.getStatus());
        dto.setRemark(d.getRemark());
        dto.setCreatedAt(d.getCreatedAt());
        // 前端展示字段
        dto.setStatusLabel(statusLabel(d.getStatus()));
        dto.setStatusType(statusType(d.getStatus()));
        return dto;
    }

    private String statusLabel(String status) {
        if ("已完成".equals(status)) return "已完成";
        if ("部分完成".equals(status)) return "部分完成";
        return "待出库";
    }

    private String statusType(String status) {
        if ("已完成".equals(status)) return "success";
        if ("部分完成".equals(status)) return "warning";
        return "info";
    }

    private String nvl(String value, String defaultVal) {
        return (value == null || value.trim().isEmpty()) ? defaultVal : value;
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
