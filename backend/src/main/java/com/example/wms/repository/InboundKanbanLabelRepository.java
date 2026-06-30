package com.example.wms.repository;

import com.example.wms.entity.InboundKanbanLabel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InboundKanbanLabelRepository extends JpaRepository<InboundKanbanLabel, Long>, JpaSpecificationExecutor<InboundKanbanLabel> {

    List<InboundKanbanLabel> findByInboundOrderIdOrderByInboundOrderDetailIdAscPackageSeqAsc(Long inboundOrderId);

    List<InboundKanbanLabel> findByInboundOrderDetailIdIn(Collection<Long> detailIds);

    Optional<InboundKanbanLabel> findByKanbanNo(String kanbanNo);

    /**
     * 带悲观锁的看板号查询
     * 用于转包时锁定源看板，防止并发超转包
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM InboundKanbanLabel k WHERE k.kanbanNo = :kanbanNo")
    Optional<InboundKanbanLabel> findByKanbanNoWithLock(@Param("kanbanNo") String kanbanNo);

    /**
     * 原子扣减看板数量
     * 返回受影响行数，0表示库存不足或已并发修改
     * 条件：labelQty >= transferQty 确保不会扣成负数
     */
    @Modifying
    @Transactional
    @Query("UPDATE InboundKanbanLabel k SET k.labelQty = k.labelQty - :qty " +
           "WHERE k.kanbanNo = :kanbanNo AND k.labelQty >= :qty")
    int decreaseLabelQty(@Param("kanbanNo") String kanbanNo, @Param("qty") int qty);

    /**
     * 原子增加目标看板数量（合包时使用）
     */
    @Modifying
    @Transactional
    @Query("UPDATE InboundKanbanLabel k SET k.labelQty = k.labelQty + :qty " +
           "WHERE k.kanbanNo = :kanbanNo AND k.labelStatus = '已入库' AND (k.sealed = false OR k.sealed IS NULL)")
    int increaseLabelQty(@Param("kanbanNo") String kanbanNo, @Param("qty") int qty);

    boolean existsByKanbanNo(String kanbanNo);

    List<InboundKanbanLabel> findByMaterialCodeAndSupplierNameOrderByCreatedAtAsc(String materialCode, String supplierName);

    List<InboundKanbanLabel> findByMaterialCodeAndSupplierNameAndWarehouseAreaOrderByCreatedAtAsc(
            String materialCode, String supplierName, String warehouseArea);

    /** 查询指定物料+供应商下所有已封存的看板标签 */
    List<InboundKanbanLabel> findByMaterialCodeAndSupplierNameAndSealedTrue(String materialCode, String supplierName);

    List<InboundKanbanLabel> findByMaterialCodeAndSupplierNameAndWarehouseAreaAndSealedTrue(
            String materialCode, String supplierName, String warehouseArea);

    /** 查询所有已封存的看板标签 */
    List<InboundKanbanLabel> findBySealedTrue();

    /** 分页查询已入库、未封存、未出库的看板（可按物料编码和供应商名称筛选） */
    @Query("SELECT k FROM InboundKanbanLabel k WHERE k.labelStatus = :labelStatus AND (k.sealed = false OR k.sealed IS NULL) AND (k.transferStatus IS NULL OR k.transferStatus != '已出库')")
    Page<InboundKanbanLabel> findByLabelStatusAndSealedFalse(@Param("labelStatus") String labelStatus, Pageable pageable);

    /** 分页查询已入库、未封存、未出库的看板（按物料编码筛选） */
    @Query("SELECT k FROM InboundKanbanLabel k WHERE k.labelStatus = :labelStatus AND (k.sealed = false OR k.sealed IS NULL) AND (k.transferStatus IS NULL OR k.transferStatus != '已出库') AND k.materialCode LIKE CONCAT('%', :materialCode, '%')")
    Page<InboundKanbanLabel> findByLabelStatusAndSealedFalseAndMaterialCodeContaining(
            @Param("labelStatus") String labelStatus, @Param("materialCode") String materialCode, Pageable pageable);

    /** 分页查询已入库、未封存、未出库的看板（按供应商名称筛选） */
    @Query("SELECT k FROM InboundKanbanLabel k WHERE k.labelStatus = :labelStatus AND (k.sealed = false OR k.sealed IS NULL) AND (k.transferStatus IS NULL OR k.transferStatus != '已出库') AND k.supplierName LIKE CONCAT('%', :supplierName, '%')")
    Page<InboundKanbanLabel> findByLabelStatusAndSealedFalseAndSupplierNameContaining(
            @Param("labelStatus") String labelStatus, @Param("supplierName") String supplierName, Pageable pageable);

    /** 分页查询已入库、未封存、未出库的看板（按物料编码和供应商名称筛选） */
    @Query("SELECT k FROM InboundKanbanLabel k WHERE k.labelStatus = :labelStatus AND (k.sealed = false OR k.sealed IS NULL) AND (k.transferStatus IS NULL OR k.transferStatus != '已出库') AND k.materialCode LIKE CONCAT('%', :materialCode, '%') AND k.supplierName LIKE CONCAT('%', :supplierName, '%')")
    Page<InboundKanbanLabel> findByLabelStatusAndSealedFalseAndMaterialCodeContainingAndSupplierNameContaining(
            @Param("labelStatus") String labelStatus,
            @Param("materialCode") String materialCode,
            @Param("supplierName") String supplierName,
            Pageable pageable);
}
