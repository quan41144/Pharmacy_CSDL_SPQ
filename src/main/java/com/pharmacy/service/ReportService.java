package com.pharmacy.service;

import com.pharmacy.dto.DashboardKPIsDTO;
import com.pharmacy.dto.InventoryReportItemDTO;
import com.pharmacy.dto.InvoiceResponseDTO;
import com.pharmacy.dto.ImportSlipResponseDTO;
import com.pharmacy.entity.Inventory;
import com.pharmacy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final ImportSlipRepository importSlipRepository;
    private final DisposalSlipRepository disposalSlipRepository;
    private final InventoryRepository inventoryRepository;
    private final EmployeeService employeeService;
    private final InvoiceService invoiceService;
    private final ImportSlipService importSlipService;

    @Transactional(readOnly = true)
    public DashboardKPIsDTO getKPIs(String filterType, String filterValue) {
        LocalDateTime[] range = parseDateRange(filterType, filterValue);
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        BigDecimal totalImport = importSlipRepository.sumImportAmountByDateRange(start, end);
        BigDecimal totalRevenue = invoiceRepository.sumPaidAmountByDateRange(start, end);

        if (totalImport == null) totalImport = BigDecimal.ZERO;
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // Ước tính giá vốn hàng bán: khoảng 65% doanh thu hoặc chênh lệch doanh thu - nhập kho
        BigDecimal costOfGoodsSold = totalRevenue.multiply(BigDecimal.valueOf(0.65));
        BigDecimal grossProfit = totalRevenue.subtract(costOfGoodsSold);

        BigDecimal adminBalance = employeeService.getAdminBalance();

        long invoicesCount = invoiceRepository.findByDateRange(start, end).size();
        long importsCount = importSlipRepository.findByDateRange(start, end).size();
        long disposalsCount = disposalSlipRepository.findAll().size();

        return DashboardKPIsDTO.builder()
                .totalImportAmount(totalImport)
                .totalRevenue(totalRevenue)
                .grossProfit(grossProfit)
                .adminBalance(adminBalance)
                .filterPeriod(formatPeriodLabel(filterType, filterValue))
                .totalInvoicesCount(invoicesCount)
                .totalImportsCount(importsCount)
                .totalDisposalsCount(disposalsCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponseDTO> getInvoicesInPeriod(String filterType, String filterValue) {
        LocalDateTime[] range = parseDateRange(filterType, filterValue);
        return invoiceRepository.findByDateRange(range[0], range[1]).stream()
                .map(invoiceService::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ImportSlipResponseDTO> getImportSlipsInPeriod(String filterType, String filterValue) {
        LocalDateTime[] range = parseDateRange(filterType, filterValue);
        return importSlipRepository.findByDateRange(range[0], range[1]).stream()
                .map(importSlipService::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryReportItemDTO> getInventoryReport(Long warehouseId, String keyword) {
        List<Inventory> list = inventoryRepository.findAllInventoryDetails(warehouseId, keyword);
        List<InventoryReportItemDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Inventory inv : list) {
            LocalDate exp = inv.getBatch().getExpDate();
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, exp);

            String status;
            if (daysRemaining <= 0) {
                status = "Hết hạn";
            } else if (daysRemaining <= 30) {
                status = "Gần hết hạn";
            } else {
                status = "Còn hạn";
            }

            BigDecimal totalVal = inv.getBatch().getProduct().getSellingPrice().multiply(BigDecimal.valueOf(inv.getQuantity()));

            result.add(InventoryReportItemDTO.builder()
                    .warehouseId(inv.getWarehouse().getWarehouseId())
                    .warehouseName(inv.getWarehouse().getWarehouseName())
                    .batchId(inv.getBatch().getBatchId())
                    .batchCode(inv.getBatch().getBatchCode())
                    .productId(inv.getBatch().getProduct().getProductId())
                    .productName(inv.getBatch().getProduct().getProductName())
                    .categoryName(inv.getBatch().getProduct().getCategory() != null ? inv.getBatch().getProduct().getCategory().getCategoryName() : "")
                    .unit(inv.getBatch().getProduct().getUnit())
                    .mfgDate(inv.getBatch().getMfgDate())
                    .expDate(exp)
                    .quantity(inv.getQuantity())
                    .sellingPrice(inv.getBatch().getProduct().getSellingPrice())
                    .totalValue(totalVal)
                    .daysRemaining(daysRemaining)
                    .expiryStatus(status)
                    .build());
        }
        return result;
    }

    public LocalDateTime[] parseDateRange(String filterType, String filterValue) {
        LocalDate today = LocalDate.now();
        if (filterType == null || filterType.isBlank()) {
            filterType = "MONTH";
        }

        LocalDateTime start;
        LocalDateTime end;

        try {
            if ("DAY".equalsIgnoreCase(filterType)) {
                LocalDate d = (filterValue != null && !filterValue.isBlank()) ?
                        (filterValue.contains("/") ? LocalDate.parse(filterValue, DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDate.parse(filterValue)) :
                        today;
                start = d.atStartOfDay();
                end = d.atTime(LocalTime.MAX);
            } else if ("YEAR".equalsIgnoreCase(filterType)) {
                int y = (filterValue != null && !filterValue.isBlank()) ? Integer.parseInt(filterValue) : today.getYear();
                start = LocalDate.of(y, 1, 1).atStartOfDay();
                end = LocalDate.of(y, 12, 31).atTime(LocalTime.MAX);
            } else { // MONTH
                int m = today.getMonthValue();
                int y = today.getYear();
                if (filterValue != null && !filterValue.isBlank()) {
                    if (filterValue.contains("/")) {
                        String[] parts = filterValue.split("/");
                        m = Integer.parseInt(parts[0]);
                        y = Integer.parseInt(parts[1]);
                    } else if (filterValue.contains("-")) {
                        String[] parts = filterValue.split("-");
                        y = Integer.parseInt(parts[0]);
                        m = Integer.parseInt(parts[1]);
                    }
                }
                LocalDate firstDay = LocalDate.of(y, m, 1);
                LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
                start = firstDay.atStartOfDay();
                end = lastDay.atTime(LocalTime.MAX);
            }
        } catch (Exception e) {
            start = today.withDayOfMonth(1).atStartOfDay();
            end = today.atTime(LocalTime.MAX);
        }

        return new LocalDateTime[]{start, end};
    }

    private String formatPeriodLabel(String filterType, String filterValue) {
        if ("DAY".equalsIgnoreCase(filterType)) {
            return "Ngày: " + (filterValue != null ? filterValue : LocalDate.now().toString());
        } else if ("YEAR".equalsIgnoreCase(filterType)) {
            return "Năm: " + (filterValue != null ? filterValue : String.valueOf(LocalDate.now().getYear()));
        } else {
            return "Tháng: " + (filterValue != null ? filterValue : (LocalDate.now().getMonthValue() + "/" + LocalDate.now().getYear()));
        }
    }
}
