package com.example.wms.repository;

import com.example.wms.entity.WarehouseArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseAreaRepository extends JpaRepository<WarehouseArea, Long> {

    List<WarehouseArea> findAllByOrderBySortOrderAsc();
}
