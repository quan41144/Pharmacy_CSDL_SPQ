package com.pharmacy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponseDTO {
    private Long invoiceId;
    private String invoiceCode; // e.g. "PT00001"
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private Long salesStaffId;
    private String salesStaffName;
    private Long warehouseId;
    private String warehouseName;
    private String warehouseAddress;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private String totalAmountInWords;
    private String status; // UNPAID, PAID, CANCELLED
    private String note;
    private List<InvoiceItemResponseDTO> items;
}
