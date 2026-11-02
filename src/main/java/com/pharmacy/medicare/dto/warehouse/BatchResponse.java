package com.pharmacy.medicare.dto.warehouse;

import com.pharmacy.medicare.entity.Batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BatchResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String unit;
    private Integer productTotalStock;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private Integer quantity;
    private BigDecimal importPrice;
    private BigDecimal currentSellingPrice;
    private String status;
    private Boolean isDamaged;
    private Boolean isExpired;
    private Boolean isEligibleForDisposal;
    private Long warehouseId;
    private String warehouseName;
    private String warehouseAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BatchResponse() {
    }

    public static BatchResponse fromEntity(Batch b) {
        BatchResponse res = new BatchResponse();
        res.setId(b.getId());
        if (b.getProduct() != null) {
            res.setProductId(b.getProduct().getId());
            res.setProductName(b.getProduct().getName());
            res.setUnit(b.getProduct().getUnit());
            res.setCurrentSellingPrice(b.getProduct().getSellingPrice());
            res.setProductTotalStock(b.getProduct().getTotalStock());
        }
        res.setMfgDate(b.getMfgDate());
        res.setExpDate(b.getExpDate());
        res.setQuantity(b.getQuantity());
        res.setImportPrice(b.getImportPrice());
        res.setStatus(b.getStatus() != null ? b.getStatus().name() : null);
        res.setIsDamaged(b.getIsDamaged());
        res.setIsExpired(b.isExpired());
        res.setIsEligibleForDisposal(b.isEligibleForDisposal());
        res.setCreatedAt(b.getCreatedAt());
        res.setUpdatedAt(b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt());

        if (b.getWarehouse() != null) {
            res.setWarehouseId(b.getWarehouse().getId());
            res.setWarehouseName(b.getWarehouse().getWarehouseName());
            res.setWarehouseAddress(b.getWarehouse().getWarehouseAddress());
        } else {
            res.setWarehouseName("Kho Dược Phẩm Trung Tâm");
            res.setWarehouseAddress("Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội");
        }

        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getProductTotalStock() {
        return productTotalStock;
    }

    public void setProductTotalStock(Integer productTotalStock) {
        this.productTotalStock = productTotalStock;
    }

    public LocalDate getMfgDate() {
        return mfgDate;
    }

    public void setMfgDate(LocalDate mfgDate) {
        this.mfgDate = mfgDate;
    }

    public LocalDate getExpDate() {
        return expDate;
    }

    public void setExpDate(LocalDate expDate) {
        this.expDate = expDate;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = importPrice;
    }

    public BigDecimal getCurrentSellingPrice() {
        return currentSellingPrice;
    }

    public void setCurrentSellingPrice(BigDecimal currentSellingPrice) {
        this.currentSellingPrice = currentSellingPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsDamaged() {
        return isDamaged;
    }

    public void setIsDamaged(Boolean isDamaged) {
        this.isDamaged = isDamaged;
    }

    public Boolean getIsExpired() {
        return isExpired;
    }

    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }

    public Boolean getIsEligibleForDisposal() {
        return isEligibleForDisposal;
    }

    public void setIsEligibleForDisposal(Boolean isEligibleForDisposal) {
        this.isEligibleForDisposal = isEligibleForDisposal;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
