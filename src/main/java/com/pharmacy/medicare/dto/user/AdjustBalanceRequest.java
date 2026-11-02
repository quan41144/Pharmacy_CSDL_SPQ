package com.pharmacy.medicare.dto.user;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AdjustBalanceRequest {

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "1000", message = "Số tiền tối thiểu là 1,000 VND")
    private BigDecimal amount;

    private String note;

    public AdjustBalanceRequest() {
    }

    public AdjustBalanceRequest(BigDecimal amount, String note) {
        this.amount = amount;
        this.note = note;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
