package com.pharmacy.medicare.controller;

import com.pharmacy.medicare.dto.ApiResponse;
import com.pharmacy.medicare.dto.invoice.CreateInvoiceRequest;
import com.pharmacy.medicare.dto.invoice.InvoiceResponse;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.service.AuthService;
import com.pharmacy.medicare.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/invoices", "/api/invoices"})
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final AuthService authService;

    public InvoiceController(InvoiceService invoiceService, AuthService authService) {
        this.invoiceService = invoiceService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        InvoiceResponse response = invoiceService.createInvoice(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Hóa đơn đã được tạo thành công! Vui lòng chờ nhân viên xác nhận trong 10 phút.", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getMyInvoices() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        List<InvoiceResponse> list = invoiceService.getMyInvoices(currentUser);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        InvoiceResponse response = invoiceService.getInvoiceById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancelInvoice(@PathVariable Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        InvoiceResponse response = invoiceService.cancelInvoice(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Hóa đơn đã được hủy thành công!", response));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        byte[] pdfBytes = invoiceService.getInvoicePdf(id, currentUser);

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
