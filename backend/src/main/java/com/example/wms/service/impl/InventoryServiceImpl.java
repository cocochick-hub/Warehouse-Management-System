package com.example.wms.service.impl;

import com.example.wms.dto.inbound.InboundKanbanLabelDTO;
import com.example.wms.dto.inbound.InventoryStockDTO;
import com.example.wms.dto.inventory.InventoryStockPageResponse;
import com.example.wms.entity.InboundKanbanLabel;
import com.example.wms.entity.InventoryStock;
import com.example.wms.repository.InboundKanbanLabelRepository;
import com.example.wms.repository.InventoryStockRepository;
import com.example.wms.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryStockRepository inventoryStockRepository;
    private final InboundKanbanLabelRepository inboundKanbanLabelRepository;

    public InventoryServiceImpl(InventoryStockRepository inventoryStockRepository,
                                InboundKanbanLabelRepository inboundKanbanLabelRepository) {
        this.inventoryStockRepository = inventoryStockRepository;
        this.inboundKanbanLabelRepository = inboundKanbanLabelRepository;
    }

    @Override
    public InventoryStockPageResponse listStocks(String materialCode, String materialName, String supplier,
                                                  String transferStatus, String warehouseArea,
                                                  Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : size;
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "lastInboundAt"));
        Page<InventoryStock> resultPage = inventoryStockRepository.findAll(
                buildSpecification(materialCode, materialName, supplier, transferStatus, warehouseArea), pageable);
        List<InventoryStockDTO> records = resultPage.getContent().stream()
                .map(item -> {
                    int onHandQty = safeInt(item.getOnHandQty());
                    int sealedQty = calculateSealedQty(item.getMaterialCode(), item.getSupplier(), item.getWarehouseArea());
                    int availableQty = Math.max(onHandQty - sealedQty, 0);
                    return new InventoryStockDTO(
                            item.getMaterialCode(),
                            item.getMaterialName(),
                            item.getSupplier(),
                            onHandQty,
                            item.getLastInboundDocNo(),
                            item.getLastInboundAt(),
                            item.getTransferStatus(),
                            item.getWarehouseArea(),
                            availableQty
                    );
                })
                .collect(Collectors.toList());
        return new InventoryStockPageResponse((int) resultPage.getTotalElements(), safePage, safeSize, records);
    }

    private Specification<InventoryStock> buildSpecification(String materialCode, String materialName, String supplier,
                                                              String transferStatus, String warehouseArea) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String materialCodeKeyword = trimToNull(materialCode);
            String materialNameKeyword = trimToNull(materialName);
            String supplierKeyword = trimToNull(supplier);
            String tsKeyword = trimToNull(transferStatus);
            String waKeyword = trimToNull(warehouseArea);

            if (materialCodeKeyword != null) {
                predicates.add(criteriaBuilder.like(root.get("materialCode"), "%" + materialCodeKeyword + "%"));
            }
            if (materialNameKeyword != null) {
                predicates.add(criteriaBuilder.like(root.get("materialName"), "%" + materialNameKeyword + "%"));
            }
            if (supplierKeyword != null) {
                predicates.add(criteriaBuilder.like(root.get("supplier"), "%" + supplierKeyword + "%"));
            }
            if (tsKeyword != null) {
                predicates.add(criteriaBuilder.equal(root.get("transferStatus"), tsKeyword));
            }
            if (waKeyword != null) {
                predicates.add(criteriaBuilder.like(root.get("warehouseArea"), "%" + waKeyword + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public List<InboundKanbanLabelDTO> listLabelsByMaterial(String materialCode, String supplier, String warehouseArea) {
        String area = trimToNull(warehouseArea);
        List<InboundKanbanLabel> labels = area == null
                ? inboundKanbanLabelRepository.findByMaterialCodeAndSupplierNameOrderByCreatedAtAsc(
                        materialCode.trim(), supplier.trim())
                : inboundKanbanLabelRepository.findByMaterialCodeAndSupplierNameAndWarehouseAreaOrderByCreatedAtAsc(
                        materialCode.trim(), supplier.trim(), area);
        return labels.stream()
                .map(this::toKanbanLabelDTO)
                .collect(Collectors.toList());
    }

    private InboundKanbanLabelDTO toKanbanLabelDTO(InboundKanbanLabel label) {
        int availableQty = calculateAvailableQty(label);
        return new InboundKanbanLabelDTO(
                label.getId(),
                label.getInboundOrderId(),
                label.getInboundOrderDetailId(),
                label.getDocNo(),
                label.getKanbanNo(),
                label.getQrPayload(),
                label.getMaterialCode(),
                label.getMaterialName(),
                label.getSupplierCode(),
                label.getSupplierName(),
                label.getPackageModel(),
                label.getWarehouseArea(),
                label.getLabelQty(),
                label.getPackageSeq(),
                label.getPackageTotal(),
                label.getTransferStatus(),
                label.getLabelStatus(),
                label.getPrintedAt(),
                label.getReceivedAt(),
                label.getReceivedBy(),
                label.getSealed(),
                label.getSealedAt(),
                label.getSealedBy(),
                availableQty
        );
    }

    private int calculateAvailableQty(InboundKanbanLabel label) {
        int labelQty = safeInt(label.getLabelQty());
        int frozenQty = safeInt(label.getFrozenQty());
        return Math.max(labelQty - frozenQty, 0);
    }

    private int calculateSealedQty(String materialCode, String supplierName, String warehouseArea) {
        String area = trimToNull(warehouseArea);
        List<InboundKanbanLabel> sealedLabels = area == null
                ? inboundKanbanLabelRepository.findByMaterialCodeAndSupplierNameAndSealedTrue(materialCode, supplierName)
                : inboundKanbanLabelRepository.findByMaterialCodeAndSupplierNameAndWarehouseAreaAndSealedTrue(
                        materialCode, supplierName, area);
        return sealedLabels
                .stream()
                .mapToInt(this::sealedQtyOf)
                .sum();
    }

    private int sealedQtyOf(InboundKanbanLabel label) {
        int frozenQty = safeInt(label.getFrozenQty());
        return frozenQty > 0 ? frozenQty : safeInt(label.getLabelQty());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
