package com.pharmacy.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardKPIsDTO {
    private BigDecimal totalImportAmount;
    private BigDecimal totalRevenue;
    private BigDecimal grossProfit;
    private BigDecimal adminBalance;
    private String filterPeriod;
    private long totalInvoicesCount;
    private long totalImportsCount;
    private long totalDisposalsCount;
}
