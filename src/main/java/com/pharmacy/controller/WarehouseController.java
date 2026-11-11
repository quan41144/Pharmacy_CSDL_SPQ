package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.WarehouseDTO;
import com.pharmacy.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getAllWarehouses() {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getAllWarehouses()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getActiveWarehouses() {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getActiveWarehouses()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseDTO>> saveWarehouse(@RequestBody WarehouseDTO dto) {
        try {
            WarehouseDTO saved = warehouseService.saveWarehouse(dto);
            return ResponseEntity.ok(ApiResponse.ok("Lưu kho thành công", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(extractErrorMessage(e)));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseDTO>> toggleWarehouseStatus(@PathVariable Long id) {
        try {
            WarehouseDTO updated = warehouseService.toggleWarehouseStatus(id);
            return ResponseEntity.ok(ApiResponse.ok("Đổi trạng thái kho thành công", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(extractErrorMessage(e)));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteWarehouse(@PathVariable Long id) {
        try {
            warehouseService.deleteWarehouse(id);
            return ResponseEntity.ok(ApiResponse.ok("Xóa kho hàng thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(extractErrorMessage(e)));
        }
    }

    private String extractErrorMessage(Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("ERROR:")) {
            int start = msg.indexOf("ERROR:") + 6;
            int end = msg.indexOf("\n", start);
            if (end > start) {
                msg = msg.substring(start, end).trim();
            } else {
                msg = msg.substring(start).trim();
            }
        }
        return msg != null ? msg : "Đã có lỗi xảy ra";
    }
}