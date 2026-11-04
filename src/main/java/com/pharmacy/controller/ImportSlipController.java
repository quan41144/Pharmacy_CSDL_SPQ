package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.CreateImportSlipRequest;
import com.pharmacy.dto.ImportSlipResponseDTO;
import com.pharmacy.service.ImportSlipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/import-slips")
@RequiredArgsConstructor
public class ImportSlipController {

    private final ImportSlipService importSlipService;

    @PostMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
    public ResponseEntity<ApiResponse<ImportSlipResponseDTO>> createImportSlip(@Valid @RequestBody CreateImportSlipRequest request) {
        try {
            ImportSlipResponseDTO response = importSlipService.createImportSlip(request);
            return ResponseEntity.ok(ApiResponse.ok("Đã gửi yêu cầu xác nhận đến Chủ cửa hàng", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImportSlipResponseDTO>> confirmImportSlip(@PathVariable Long id) {
        try {
            ImportSlipResponseDTO response = importSlipService.confirmImportSlip(id);
            return ResponseEntity.ok(ApiResponse.ok("Phê duyệt phiếu nhập thành công (Đã cộng tồn kho & trừ số dư)", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImportSlipResponseDTO>> cancelImportSlip(@PathVariable Long id) {
        try {
            ImportSlipResponseDTO response = importSlipService.cancelImportSlip(id);
            return ResponseEntity.ok(ApiResponse.ok("Hủy phiếu nhập thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ImportSlipResponseDTO>>> getAllImportSlips() {
        return ResponseEntity.ok(ApiResponse.ok(importSlipService.getAllImportSlips()));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ImportSlipResponseDTO>>> getPendingImportSlips() {
        return ResponseEntity.ok(ApiResponse.ok(importSlipService.getPendingImportSlips()));
    }
}
