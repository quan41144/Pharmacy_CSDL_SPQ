package com.pharmacy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportSlipItemRequest {
    @NotNull(message = "Thuốc cần nhập không được để trống")
    private Long productId;

    @NotBlank(message = "Số lô không được để trống")
    private String batchCode;

    @NotNull(message = "Ngày sản xuất không được để trống")
    private LocalDate mfgDate;

    @NotNull(message = "Hạn sử dụng không được để trống")
    private LocalDate expDate;

    @NotNull(message = "Số lượng nhập không được để trống")
    @Min(value = 1, message = "Số lượng nhập phải lớn hơn 0")
    private Integer quantity;

    @NotNull(message = "Đơn giá nhập không được để trống")
    @DecimalMin(value = "0.0", message = "Đơn giá nhập phải >= 0")
    private BigDecimal unitPrice;
}
