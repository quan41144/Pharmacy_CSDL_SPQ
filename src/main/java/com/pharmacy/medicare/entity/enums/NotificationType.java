package com.pharmacy.medicare.entity.enums;

public enum NotificationType {
    PAYMENT_CONFIRM,    // Yêu cầu xác nhận thanh toán (từ Khách gửi NV Bán hàng)
    IMPORT_CONFIRM,     // Yêu cầu duyệt phiếu nhập (từ NV Kho gửi Admin)
    DISPOSAL_CONFIRM    // Yêu cầu duyệt phiếu hủy (từ NV Kho gửi Admin)
}
