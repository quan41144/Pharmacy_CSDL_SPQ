package com.pharmacy.medicare.controller;

import com.pharmacy.medicare.dto.ApiResponse;
import com.pharmacy.medicare.dto.invoice.ConfirmPaymentResponse;
import com.pharmacy.medicare.dto.invoice.InvoiceResponse;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.service.AuthService;
import com.pharmacy.medicare.service.InvoiceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales/invoices")
@PreAuthorize("hasAnyRole('SALES', 'ADMIN')")
public class SalesInvoiceController {

    private final InvoiceService invoiceService;
    private final AuthService authService;

    public SalesInvoiceController(InvoiceService invoiceService, AuthService authService) {
        this.invoiceService = invoiceService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getAllInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        List<InvoiceResponse> list = invoiceService.getAllInvoices(status, keyword);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hóa đơn thành công", list));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<ConfirmPaymentResponse>> confirmInvoicePayment(@PathVariable Long id) {
        User staffUser = authService.getCurrentAuthenticatedUser();
        ConfirmPaymentResponse result = invoiceService.confirmPayment(id, staffUser);

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(result.getMessage(), result));
        }
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), result));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        User staffUser = authService.getCurrentAuthenticatedUser();
        byte[] pdfBytes = invoiceService.getInvoicePdf(id, staffUser);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.inline()
                .filename("HoaDon_" + id + ".pdf", java.nio.charset.StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
