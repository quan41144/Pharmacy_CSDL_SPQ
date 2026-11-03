package com.pharmacy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportSlipItemResponseDTO {
    private Long batchId;
    private String batchCode;
    private Long productId;
    private String productName;
    private String unit;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
