package com.example.wms.repository;

import com.example.wms.entity.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long>, JpaSpecificationExecutor<InventoryStock> {

    Optional<InventoryStock> findByMaterialCodeAndSupplier(String materialCode, String supplier);

    /** 按物料号+供应商+库区查询库存 */
    Optional<InventoryStock> findByMaterialCodeAndSupplierAndWarehouseArea(String materialCode, String supplier, String warehouseArea);

    /** 按物料号+供应商查询所有库区的库存 */
    List<InventoryStock> findAllByMaterialCodeAndSupplier(String materialCode, String supplier);

    List<InventoryStock> findBySupplierAndMaterialCodeIn(String supplier, Collection<String> materialCodes);

    int countByOnHandQty(int onHandQty);
}
