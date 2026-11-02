package com.pharmacy.medicare.entity.enums;

public enum BatchStatus {
    PENDING_APPROVAL,   // Chờ chủ duyệt nhập
    WAITING_FOR_PRICE,  // Đã nhập nhưng chờ chủ duyệt giá bán
    AVAILABLE,          // Đang mở bán
    CANCELLED,          // Đã hủy
    REJECTED            // Từ chối nhập
}
