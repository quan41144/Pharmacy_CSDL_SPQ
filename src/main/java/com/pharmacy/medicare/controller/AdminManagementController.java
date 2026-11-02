package com.pharmacy.medicare.controller;

import com.pharmacy.medicare.dto.ApiResponse;
import com.pharmacy.medicare.dto.statistics.FinancialStatisticsResponse;
import com.pharmacy.medicare.dto.warehouse.ApproveBatchPriceRequest;
import com.pharmacy.medicare.dto.warehouse.BatchResponse;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.service.AdminApprovalService;
import com.pharmacy.medicare.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagementController {

    private final AdminApprovalService adminApprovalService;
    private final AuthService authService;

    public AdminManagementController(AdminApprovalService adminApprovalService, AuthService authService) {
        this.adminApprovalService = adminApprovalService;
        this.authService = authService;
    }

    @GetMapping("/batches/pending-price")
    public ResponseEntity<ApiResponse<List<BatchResponse>>> getBatchesWaitingForPrice() {
        List<BatchResponse> list = adminApprovalService.getBatchesWaitingForPrice();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lô hàng chờ duyệt giá bán thành công", list));
    }

    @PostMapping("/batches/{batchId}/approve-price")
    public ResponseEntity<ApiResponse<BatchResponse>> approveBatchPrice(
            @PathVariable Long batchId,
            @Valid @RequestBody ApproveBatchPriceRequest request) {
        User admin = authService.getCurrentAuthenticatedUser();
        BatchResponse res = adminApprovalService.approveBatchPrice(batchId, request.getSellingPrice(), admin);
        return ResponseEntity.ok(ApiResponse.success("Duyệt bán và cập nhật giá bán thành công cho lô thuốc!", res));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<FinancialStatisticsResponse>> getFinancialStatistics(
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false) String date) {
        FinancialStatisticsResponse stats = adminApprovalService.getFinancialStatistics(period, date);
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo thống kê doanh thu thành công", stats));
    }
}
