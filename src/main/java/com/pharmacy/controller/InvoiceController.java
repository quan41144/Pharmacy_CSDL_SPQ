package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.CreateInvoiceRequest;
import com.pharmacy.dto.InvoiceResponseDTO;
import com.pharmacy.service.InvoiceService;
import com.pharmacy.service.PdfExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PdfExportService pdfExportService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SALES', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        try {
            InvoiceResponseDTO response = invoiceService.createInvoice(request);
            return ResponseEntity.ok(ApiResponse.ok("Tạo hóa đơn thành công (chờ thanh toán)", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/confirm-payment")
    @PreAuthorize("hasAnyRole('SALES', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> confirmPayment(@PathVariable Long id) {
        try {
            InvoiceResponseDTO response = invoiceService.confirmPayment(id);
            return ResponseEntity.ok(ApiResponse.ok("Xác nhận thanh toán thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SALES', 'ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> cancelInvoice(@PathVariable Long id) {
        try {
            InvoiceResponseDTO response = invoiceService.cancelInvoice(id);
            return ResponseEntity.ok(ApiResponse.ok("Hủy hóa đơn thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> getInvoiceById(@PathVariable Long id) {
        try {
            InvoiceResponseDTO response = invoiceService.getInvoiceById(id);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceResponseDTO>>> getAllInvoices(
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.searchInvoices(keyword)));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> getInvoicePdf(@PathVariable Long id) {
        InvoiceResponseDTO invoice = invoiceService.getInvoiceById(id);
        if ("CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Hóa đơn đã hủy không được phép in hoặc xuất PDF."));
        }
        byte[] pdfBytes = pdfExportService.exportInvoiceK80(invoice);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"HoaDon_" + invoice.getInvoiceCode() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
