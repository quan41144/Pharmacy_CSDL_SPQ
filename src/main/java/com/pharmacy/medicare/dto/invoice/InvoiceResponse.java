package com.pharmacy.medicare.dto.invoice;

import com.pharmacy.medicare.entity.Invoice;
import com.pharmacy.medicare.entity.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InvoiceResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private Long salesStaffId;
    private String salesStaffName;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String resultState;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long remainingSeconds; // Số giây còn lại cho bộ đếm ngược 10 phút trên UI
    private String note;
    private List<InvoiceItemResponse> items;

    public InvoiceResponse() {
    }

    public static InvoiceResponse fromEntity(Invoice inv) {
        InvoiceResponse res = new InvoiceResponse();
        res.setId(inv.getId());
        if (inv.getCustomer() != null) {
            res.setCustomerId(inv.getCustomer().getId());
            res.setCustomerName(inv.getCustomer().getFullName());
        }
        if (inv.getSalesStaff() != null) {
            res.setSalesStaffId(inv.getSalesStaff().getId());
            res.setSalesStaffName(inv.getSalesStaff().getFullName());
        }
        res.setTotalAmount(inv.getTotalAmount());
        res.setPaymentMethod(inv.getPaymentMethod() != null ? inv.getPaymentMethod().name() : null);
        res.setResultState(inv.getResultState());
        res.setStatus(inv.getStatus() != null ? inv.getStatus().name() : null);
        res.setCreatedAt(inv.getCreatedAt());
        res.setExpiresAt(inv.getExpiresAt());
        res.setNote(inv.getNote());

        if (inv.getStatus() == InvoiceStatus.UNPAID && inv.getExpiresAt() != null) {
            long seconds = Duration.between(LocalDateTime.now(), inv.getExpiresAt()).getSeconds();
            res.setRemainingSeconds(Math.max(0, seconds));
        } else {
            res.setRemainingSeconds(0L);
        }

        if (inv.getItems() != null) {
            res.setItems(inv.getItems().stream()
                    .map(InvoiceItemResponse::fromEntity)
                    .collect(Collectors.toList()));
        } else {
            res.setItems(Collections.emptyList());
        }
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getSalesStaffId() {
        return salesStaffId;
    }

    public void setSalesStaffId(Long salesStaffId) {
        this.salesStaffId = salesStaffId;
    }

    public String getSalesStaffName() {
        return salesStaffName;
    }

    public void setSalesStaffName(String salesStaffName) {
        this.salesStaffName = salesStaffName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<InvoiceItemResponse> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItemResponse> items) {
        this.items = items;
    }
}
