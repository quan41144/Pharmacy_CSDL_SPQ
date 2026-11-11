package com.pharmacy.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseDTO {
    private Long warehouseId;
    private String warehouseName;
    private String warehouseAddress;
    private String status;
}
