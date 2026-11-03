package com.pharmacy.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO {
    private Long employeeId;
    private String username;
    private String password; // only used on create/update if provided
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String phone;
    private LocalDate startDate;
    private String position;
    private String workStatus;
    private BigDecimal balance;
    private Long warehouseId;
    private String warehouseName;
}
