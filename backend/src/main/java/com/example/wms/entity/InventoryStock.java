package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "inventory_stock",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_stock_material_supplier",
                        columnNames = {"material_code", "supplier"}
                )
        }
)
public class InventoryStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(nullable = false, length = 100)
    private String supplier;

    @Column(name = "on_hand_qty", nullable = false)
    private Integer onHandQty;

    @Column(name = "last_inbound_doc_no", length = 50)
    private String lastInboundDocNo;

    @Column(name = "last_inbound_at")
    private LocalDateTime lastInboundAt;

    @Column(name = "transfer_status", length = 20)
    private String transferStatus;

    @Column(name = "warehouse_area", length = 100)
    private String warehouseArea;
}
