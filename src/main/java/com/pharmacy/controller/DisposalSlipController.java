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
@PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
public class DisposalSlipController {

    private final DisposalSlipService disposalSlipService;

    @PostMapping
    public ResponseEntity<ApiResponse<DisposalSlipResponseDTO>> createDisposalSlip(@Valid @RequestBody CreateDisposalSlipRequest request) {
        try {
            DisposalSlipResponseDTO response = disposalSlipService.createAndConfirmDisposalSlip(request);
            return ResponseEntity.ok(ApiResponse.ok("Thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DisposalSlipResponseDTO>>> getAllDisposalSlips() {
        return ResponseEntity.ok(ApiResponse.ok(disposalSlipService.getAllDisposalSlips()));
    }
}
