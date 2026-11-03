package com.pharmacy.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemResponseDTO {
    private Long batchId;
    private String batchCode;
    private Long productId;
    private String productName;
    private String unit;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
}
