package com.pharmacy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceRequest {
    @NotNull(message = "Khách hàng không được để trống")
    private Long customerId;

    @NotNull(message = "Kho xuất hàng không được để trống")
    private Long warehouseId;

    private String note;

    @NotEmpty(message = "Hóa đơn phải có ít nhất một mặt hàng")
    @Valid
    private List<InvoiceItemRequest> items;
}
