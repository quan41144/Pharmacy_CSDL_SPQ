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
public class ImportSlipResponseDTO {
    private Long importSlipId;
    private Long supplierId;
    private String supplierName;
    private Long warehouseStaffId;
    private String warehouseStaffName;
    private Long warehouseId;
    private String warehouseName;
    private LocalDateTime importDate;
    private BigDecimal totalAmount;
    private String status; // PENDING, IMPORTED, CANCELLED
    private List<ImportSlipItemResponseDTO> items;
}
