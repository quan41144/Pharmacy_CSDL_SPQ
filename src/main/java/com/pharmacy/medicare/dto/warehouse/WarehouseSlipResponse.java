package com.pharmacy.medicare.dto.warehouse;

import com.pharmacy.medicare.entity.WarehouseSlip;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WarehouseSlipResponse {

    private Long id;
    private String type;
    private Long warehouseStaffId;
    private String warehouseStaffName;
    private Long productId;
    private String productName;
    private Long batchId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String resultState;
    private String status;
    private Long supplierId;
    private String supplierName;
    private Long warehouseId;
    private String warehouseName;
    private String warehouseAddress;
    private String reason;
    private LocalDateTime disposalDate;
    private LocalDateTime createdAt;

    public WarehouseSlipResponse() {
    }

    public static WarehouseSlipResponse fromEntity(WarehouseSlip slip) {
        WarehouseSlipResponse res = new WarehouseSlipResponse();
        res.setId(slip.getId());
        res.setType(slip.getType() != null ? slip.getType().name() : null);
        if (slip.getWarehouseStaff() != null) {
            res.setWarehouseStaffId(slip.getWarehouseStaff().getId());
            res.setWarehouseStaffName(slip.getWarehouseStaff().getFullName());
        }
        if (slip.getProduct() != null) {
            res.setProductId(slip.getProduct().getId());
            res.setProductName(slip.getProduct().getName());
        }
        if (slip.getBatch() != null) {
            res.setBatchId(slip.getBatch().getId());
            if (slip.getWarehouse() == null && slip.getBatch().getWarehouse() != null) {
                res.setWarehouseId(slip.getBatch().getWarehouse().getId());
                res.setWarehouseName(slip.getBatch().getWarehouse().getWarehouseName());
                res.setWarehouseAddress(slip.getBatch().getWarehouse().getWarehouseAddress());
            }
        }
        res.setQuantity(slip.getQuantity());
        res.setUnitPrice(slip.getUnitPrice());
        res.setTotalAmount(slip.getTotalAmount());
        res.setResultState(slip.getResultState());
        res.setStatus(slip.getStatus() != null ? slip.getStatus().name() : null);
        res.setReason(slip.getReason());
        res.setDisposalDate(slip.getDisposalDate() != null ? slip.getDisposalDate() : slip.getCreatedAt());
        res.setCreatedAt(slip.getCreatedAt());

        if (slip.getSupplier() != null) {
            res.setSupplierId(slip.getSupplier().getId());
            res.setSupplierName(slip.getSupplier().getName());
        }

        if (slip.getWarehouse() != null) {
            res.setWarehouseId(slip.getWarehouse().getId());
            res.setWarehouseName(slip.getWarehouse().getWarehouseName());
            res.setWarehouseAddress(slip.getWarehouse().getWarehouseAddress());
        } else if (res.getWarehouseName() == null) {
            res.setWarehouseName("Kho Dược Phẩm Trung Tâm");
        }

        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getWarehouseStaffId() {
        return warehouseStaffId;
    }

    public void setWarehouseStaffId(Long warehouseStaffId) {
        this.warehouseStaffId = warehouseStaffId;
    }

    public String getWarehouseStaffName() {
        return warehouseStaffName;
    }

    public void setWarehouseStaffName(String warehouseStaffName) {
        this.warehouseStaffName = warehouseStaffName;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getResultState() {
        return resultState;
    }

    public void setResultState(String resultState) {
        this.resultState = resultState;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getDisposalDate() {
        return disposalDate;
    }

    public void setDisposalDate(LocalDateTime disposalDate) {
        this.disposalDate = disposalDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
