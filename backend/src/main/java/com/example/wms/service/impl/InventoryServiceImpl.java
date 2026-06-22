package com.example.wms.service.impl;

import com.example.wms.dto.inbound.InventoryStockDTO;
import com.example.wms.dto.inventory.InventoryStockPageResponse;
import com.example.wms.entity.InventoryStock;
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

    public InventoryServiceImpl(InventoryStockRepository inventoryStockRepository) {
        this.inventoryStockRepository = inventoryStockRepository;
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
                .map(item -> new InventoryStockDTO(
                        item.getMaterialCode(),
                        item.getMaterialName(),
                        item.getSupplier(),
                        item.getOnHandQty(),
                        item.getLastInboundDocNo(),
                        item.getLastInboundAt(),
                        item.getTransferStatus(),
                        item.getWarehouseArea()
                ))
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
