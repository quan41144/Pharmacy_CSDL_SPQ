package com.pharmacy.medicare.dto.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class WarehouseSlipRequest {

    // Dành cho Phiếu Nhập (IMPORT)
    private Long productId;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private Long supplierId;
    private Long warehouseId;

    // Dành cho Phiếu Hủy (DISPOSAL)
    private Long batchId;
    private String reason; // Lý do hủy - bắt buộc nhập tay

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải tối thiểu là 1")
    private Integer quantity;

    public WarehouseSlipRequest() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
