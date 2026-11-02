package com.pharmacy.medicare.dto.warehouse;

import com.pharmacy.medicare.entity.Warehouse;
import java.time.LocalDateTime;

public class WarehouseResponse {

    private Long id;
    private String warehouseName;
    private String warehouseAddress;
    private LocalDateTime createdAt;

    public WarehouseResponse() {
    }

    public static WarehouseResponse fromEntity(Warehouse w) {
        WarehouseResponse res = new WarehouseResponse();
        res.setId(w.getId());
        res.setWarehouseName(w.getWarehouseName());
        res.setWarehouseAddress(w.getWarehouseAddress());
        res.setCreatedAt(w.getCreatedAt());
        return res;
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
