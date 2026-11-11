package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.CreateDisposalSlipRequest;
import com.pharmacy.dto.DisposalSlipResponseDTO;
import com.pharmacy.service.DisposalSlipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/disposal-slips")
@RequiredArgsConstructor
public class DisposalSlipController {

    private final DisposalSlipService disposalSlipService;

    @PostMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
    public ResponseEntity<ApiResponse<DisposalSlipResponseDTO>> createDisposalSlip(@Valid @RequestBody CreateDisposalSlipRequest request) {
        try {
            DisposalSlipResponseDTO response = disposalSlipService.createDisposalSlip(request);
            return ResponseEntity.ok(ApiResponse.ok("Đã lập phiếu hủy thành công (Chờ Admin phê duyệt)", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DisposalSlipResponseDTO>> confirmDisposalSlip(@PathVariable Long id) {
        try {
            DisposalSlipResponseDTO response = disposalSlipService.confirmDisposalSlip(id);
            return ResponseEntity.ok(ApiResponse.ok("Phê duyệt phiếu hủy thành công (Đã trừ tồn kho)", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DisposalSlipResponseDTO>> cancelDisposalSlip(@PathVariable Long id) {
        try {
            DisposalSlipResponseDTO response = disposalSlipService.cancelDisposalSlip(id);
            return ResponseEntity.ok(ApiResponse.ok("Từ chối phiếu hủy thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DisposalSlipResponseDTO>>> getAllDisposalSlips() {
        return ResponseEntity.ok(ApiResponse.ok(disposalSlipService.getAllDisposalSlips()));
    }
}
