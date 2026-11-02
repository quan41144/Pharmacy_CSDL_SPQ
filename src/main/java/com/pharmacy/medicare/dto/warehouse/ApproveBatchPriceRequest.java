package com.pharmacy.medicare.dto.warehouse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ApproveBatchPriceRequest {

    @NotNull(message = "Giá bán mới không được để trống")
    @DecimalMin(value = "1000", message = "Giá bán tối thiểu là 1,000 VND")
    private BigDecimal sellingPrice;

    public ApproveBatchPriceRequest() {
    }

    public ApproveBatchPriceRequest(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }
}
