package com.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDisposalSlipRequest {
    @NotNull(message = "Kho chứa không được để trống")
    private Long warehouseId;

    @NotNull(message = "Lô hàng không được để trống")
    private Long batchId;

    @NotNull(message = "Số lượng hủy không được để trống")
    @Min(value = 1, message = "Số lượng hủy phải lớn hơn 0")
    private Integer quantity;

    @NotBlank(message = "Lý do hủy không được để trống")
    private String reason;
}
