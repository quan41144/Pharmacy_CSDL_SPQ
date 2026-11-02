package com.pharmacy.medicare.dto.statistics;

import com.pharmacy.medicare.dto.invoice.InvoiceResponse;
import com.pharmacy.medicare.dto.warehouse.WarehouseSlipResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class FinancialStatisticsResponse {

    private String period; // "day", "month", "year"
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalRevenue;        // Tổng doanh thu (hóa đơn PAID / true)
    private BigDecimal totalImportExpense;  // Tổng chi phí nhập hàng (phiếu nhập APPROVED / true)
    private BigDecimal netProfit;           // Tiền lãi = Doanh thu - Chi phí nhập
    private BigDecimal adminCurrentBalance; // Số dư hiện tại của Chủ cửa hàng
    private int paidInvoicesCount;
    private int importSlipsCount;
    private List<InvoiceResponse> recentPaidInvoices;
    private List<WarehouseSlipResponse> recentImportSlips;

    public FinancialStatisticsResponse() {
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTotalImportExpense() {
        return totalImportExpense;
    }

    public void setTotalImportExpense(BigDecimal totalImportExpense) {
        this.totalImportExpense = totalImportExpense;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(BigDecimal netProfit) {
        this.netProfit = netProfit;
    }

    public BigDecimal getAdminCurrentBalance() {
        return adminCurrentBalance;
    }

    public void setAdminCurrentBalance(BigDecimal adminCurrentBalance) {
        this.adminCurrentBalance = adminCurrentBalance;
    }

    public int getPaidInvoicesCount() {
        return paidInvoicesCount;
    }

    public void setPaidInvoicesCount(int paidInvoicesCount) {
        this.paidInvoicesCount = paidInvoicesCount;
    }

    public int getImportSlipsCount() {
        return importSlipsCount;
    }

    public void setImportSlipsCount(int importSlipsCount) {
        this.importSlipsCount = importSlipsCount;
    }

    public List<InvoiceResponse> getRecentPaidInvoices() {
        return recentPaidInvoices;
    }

    public void setRecentPaidInvoices(List<InvoiceResponse> recentPaidInvoices) {
        this.recentPaidInvoices = recentPaidInvoices;
    }

    public List<WarehouseSlipResponse> getRecentImportSlips() {
        return recentImportSlips;
    }

    public void setRecentImportSlips(List<WarehouseSlipResponse> recentImportSlips) {
        this.recentImportSlips = recentImportSlips;
    }
}
