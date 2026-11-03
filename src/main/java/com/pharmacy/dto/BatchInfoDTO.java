package com.pharmacy.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchInfoDTO {
    private Long batchId;
    private Long productId;
    private String batchCode;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private Integer quantity;
    private Long warehouseId;
    private String warehouseName;
    private String status; // VALID, NEAR_EXPIRY, EXPIRED
}
