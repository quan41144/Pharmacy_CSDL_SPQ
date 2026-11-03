package com.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseDTO {
    private Long warehouseId;

    @NotBlank(message = "Tên kho không được để trống")
    private String warehouseName;

    @NotBlank(message = "Địa chỉ kho không được để trống")
    private String warehouseAddress;

    private String status;
}
