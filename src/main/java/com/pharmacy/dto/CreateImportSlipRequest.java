package com.pharmacy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateImportSlipRequest {
    @NotNull(message = "Nhà cung cấp không được để trống")
    private Long supplierId;

    @NotNull(message = "Kho nhập không được để trống")
    private Long warehouseId;

    @Valid
    private List<ImportSlipItemRequest> items;

    // Legacy fields for backward compatibility
    private Long productId;
    private String batchCode;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private Integer quantity;
    private BigDecimal unitPrice;
}
