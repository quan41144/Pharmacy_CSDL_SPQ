package com.pharmacy.controller;

import com.pharmacy.dto.*;
import com.pharmacy.service.ExcelExportService;
import com.pharmacy.service.PdfExportService;
import com.pharmacy.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

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

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadReportPdf(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) String filterValue
    ) {
        DashboardKPIsDTO kpi = reportService.getKPIs(filterType, filterValue);
        List<InvoiceResponseDTO> invoices = reportService.getInvoicesInPeriod(filterType, filterValue);
        List<InventoryReportItemDTO> inventories = reportService.getInventoryReport(null, null);

        byte[] pdfBytes = pdfExportService.exportReportPdf(kpi, invoices, inventories);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"BaoCao_HieuThuoc.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadReportExcel(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) String filterValue
    ) {
        DashboardKPIsDTO kpi = reportService.getKPIs(filterType, filterValue);
        List<InvoiceResponseDTO> invoices = reportService.getInvoicesInPeriod(filterType, filterValue);
        List<ImportSlipResponseDTO> importSlips = reportService.getImportSlipsInPeriod(filterType, filterValue);
        List<InventoryReportItemDTO> inventories = reportService.getInventoryReport(null, null);

        byte[] excelBytes = excelExportService.exportReportExcel(kpi, invoices, importSlips, inventories);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"BaoCao_HieuThuoc.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
