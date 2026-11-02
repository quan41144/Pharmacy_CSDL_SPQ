package com.pharmacy.medicare.dto.invoice;

import com.pharmacy.medicare.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateInvoiceRequest {

    // Có ô tìm kiếm Nhân viên bán hàng theo Tên hoặc Mã NV để gán vào hóa đơn.
    // Nếu khách mua online không chọn, hệ thống tự động gán Nhân viên bán hàng đầu tiên (Nhân viên 1).
    private Long salesStaffId;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    @NotEmpty(message = "Danh sách sản phẩm trong hóa đơn không được để trống")
    @Valid
    private List<InvoiceItemRequest> items;

    private String note;

    public CreateInvoiceRequest() {
    }

    public Long getSalesStaffId() {
        return salesStaffId;
    }

    public void setSalesStaffId(Long salesStaffId) {
        this.salesStaffId = salesStaffId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<InvoiceItemRequest> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItemRequest> items) {
        this.items = items;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
