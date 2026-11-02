package com.pharmacy.medicare.entity.enums;

public enum InvoiceStatus {
    UNPAID,     // Chưa thanh toán
    PAID,       // Đã thanh toán
    CANCELLED   // Đã hủy (do timeout 10 phút hoặc khách chủ động hủy)
}
