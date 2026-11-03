package com.pharmacy.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long employeeId;
    private String username;
    private String fullName;
    private String position;
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal balance;
}
