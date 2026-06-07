package com.example.wms.repository;

import com.example.wms.entity.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {

    Optional<InventoryStock> findByMaterialCodeAndSupplier(String materialCode, String supplier);
}
