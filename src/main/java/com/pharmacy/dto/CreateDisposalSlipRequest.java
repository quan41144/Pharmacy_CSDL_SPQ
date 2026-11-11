package com.pharmacy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDisposalSlipRequest {
    @NotNull(message = "Kho chứa không được để trống")
    private Long warehouseId;

    @Valid
    private List<DisposalSlipItemRequest> items;

    // Legacy fields for backward compatibility
    private Long batchId;
    private Integer quantity;
    private String reason;
}
