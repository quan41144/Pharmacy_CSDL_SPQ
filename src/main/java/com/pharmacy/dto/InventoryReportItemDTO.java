package com.pharmacy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReportItemDTO {
    private Long warehouseId;
    private String warehouseName;
    private Long batchId;
    private String batchCode;
    private Long productId;
    private String productName;
    private String categoryName;
    private String unit;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private Integer quantity;
    private BigDecimal sellingPrice;
    private BigDecimal totalValue;
    private Long daysRemaining;
    private String expiryStatus; // VALID, NEAR_EXPIRY, EXPIRED
}
