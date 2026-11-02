package com.pharmacy.medicare.controller;

import com.pharmacy.medicare.dto.ApiResponse;
import com.pharmacy.medicare.dto.notification.NotificationActionRequest;
import com.pharmacy.medicare.dto.warehouse.*;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.service.AdminApprovalService;
import com.pharmacy.medicare.service.AuthService;
import com.pharmacy.medicare.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final AuthService authService;
    private final AdminApprovalService adminApprovalService;

    public WarehouseController(WarehouseService warehouseService, AuthService authService, AdminApprovalService adminApprovalService) {
        this.warehouseService = warehouseService;
        this.authService = authService;
        this.adminApprovalService = adminApprovalService;
    }

    @PostMapping("/slips/import")
    public ResponseEntity<ApiResponse<WarehouseSlipResponse>> createImportSlip(@Valid @RequestBody WarehouseSlipRequest request) {
        User staff = authService.getCurrentAuthenticatedUser();
        WarehouseSlipResponse res = warehouseService.createImportSlip(request, staff);

        if ("fail".equalsIgnoreCase(res.getResultState())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thành công (Số dư chủ không đủ)", res));
        }
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu nhập hàng đã được gửi đến Chủ cửa hàng (Admin) chờ duyệt!", res));
    }

    @PostMapping("/slips/disposal")
    public ResponseEntity<ApiResponse<WarehouseSlipResponse>> createDisposalSlip(@Valid @RequestBody WarehouseSlipRequest request) {
        User staff = authService.getCurrentAuthenticatedUser();
        WarehouseSlipResponse res = warehouseService.createDisposalSlip(request, staff);

        if ("fail".equalsIgnoreCase(res.getResultState())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thành công: Lô hàng không đủ điều kiện hủy hoặc vượt số lượng tồn của lô", res));
        }
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu hủy hàng đã được gửi đến Chủ cửa hàng (Admin) chờ duyệt!", res));
    }

    @GetMapping("/slips")
    public ResponseEntity<ApiResponse<List<WarehouseSlipResponse>>> getAllSlips(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        List<WarehouseSlipResponse> list = warehouseService.getAllSlips(type, status);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/slips/{id}/action")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> handleSlipAction(
            @PathVariable Long id,
            @Valid @RequestBody NotificationActionRequest request) {
        User admin = authService.getCurrentAuthenticatedUser();
        String result = adminApprovalService.handleSlipDirectAction(id, request.getDecision(), admin);
        if (result.startsWith("Không thành công")) {
            return ResponseEntity.badRequest().body(ApiResponse.error(result, result));
        }
        return ResponseEntity.ok(ApiResponse.success(result, result));
    }

    @GetMapping("/batches/disposal-candidates")
    public ResponseEntity<ApiResponse<List<BatchResponse>>> searchBatchesForDisposal(
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {
        List<BatchResponse> list = warehouseService.searchBatchesForDisposal(query);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<BatchResponse>>> getAllBatches() {
        List<BatchResponse> list = warehouseService.getAllBatches();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/batches/{id}")
    public ResponseEntity<ApiResponse<BatchResponse>> getBatchById(@PathVariable Long id) {
        BatchResponse res = warehouseService.getBatchById(id);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> getAllWarehouses() {
        List<WarehouseResponse> list = warehouseService.getAllWarehouses();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResponse>> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseResponse res = warehouseService.createWarehouse(request);
        return ResponseEntity.ok(ApiResponse.success("Thêm mới kho hàng thành công", res));
    }

    @GetMapping("/suppliers")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getAllSuppliers() {
        List<SupplierResponse> list = warehouseService.getAllSuppliers();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/suppliers/search")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> searchSuppliers(
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {
        List<SupplierResponse> list = warehouseService.searchSuppliers(query);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/suppliers")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        SupplierResponse res = warehouseService.createSupplier(request);
        return ResponseEntity.ok(ApiResponse.success("Thêm nhà cung cấp thành công", res));
    }
}
