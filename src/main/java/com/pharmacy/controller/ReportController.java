package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.DashboardKPIsDTO;
import com.pharmacy.dto.InventoryReportItemDTO;
import com.pharmacy.dto.InvoiceResponseDTO;
import com.pharmacy.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<DashboardKPIsDTO>> getKPIs(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) String filterValue
    ) {
        DashboardKPIsDTO kpi = reportService.getKPIs(filterType, filterValue);
        return ResponseEntity.ok(ApiResponse.ok(kpi));
    }

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<InvoiceResponseDTO>>> getInvoicesInPeriod(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) String filterValue
    ) {
        List<InvoiceResponseDTO> list = reportService.getInvoicesInPeriod(filterType, filterValue);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<List<InventoryReportItemDTO>>> getInventoryReport(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String keyword
    ) {
        List<InventoryReportItemDTO> list = reportService.getInventoryReport(warehouseId, keyword);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}
