package com.pharmacy.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisposalSlipItemResponseDTO {
    private Long batchId;
    private String batchCode;
    private Long productId;
    private String productName;
    private String unit;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private Integer quantity;
    private String reason;
}
