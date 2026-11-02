package com.pharmacy.medicare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouses")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Long id;

    @Column(name = "warehouse_name", length = 150, nullable = false)
    private String warehouseName;

    @Column(name = "warehouse_address", length = 255, nullable = false)
    private String warehouseAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Warehouse() {
    }

    public Warehouse(String warehouseName, String warehouseAddress) {
        this.warehouseName = warehouseName;
        this.warehouseAddress = warehouseAddress;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseAddress() {
        return warehouseAddress;
    }

    public void setWarehouseAddress(String warehouseAddress) {
        this.warehouseAddress = warehouseAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
