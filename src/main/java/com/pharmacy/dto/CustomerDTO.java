package com.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private Long customerId;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotNull(message = "Ngày sinh không được để trống")
    private LocalDate dateOfBirth;

    private Integer age; // Thuộc tính dẫn xuất tính từ date_of_birth và ngày hiện tại (xét ngày và tháng sinh)

    private String gender;
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    private String note;
}
