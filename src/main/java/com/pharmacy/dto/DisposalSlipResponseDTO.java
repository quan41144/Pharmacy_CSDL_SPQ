package com.pharmacy.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisposalSlipResponseDTO {
    private Long disposalSlipId;
    private Long warehouseStaffId;
    private String warehouseStaffName;
    private Long warehouseId;
    private String warehouseName;
    private LocalDateTime disposalDate;
    private String status; // PENDING, DISPOSED, CANCELLED
    private List<DisposalSlipItemResponseDTO> items;
}
