package com.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierDTO {
    private Long supplierId;

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    private String supplierName;

    private String address;
    private String taxCode;
    private String representativeName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
}
