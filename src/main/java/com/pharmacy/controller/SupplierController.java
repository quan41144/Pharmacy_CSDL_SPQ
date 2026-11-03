package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.SupplierDTO;
import com.pharmacy.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping("/live-search")
    public ResponseEntity<ApiResponse<List<SupplierDTO>>> liveSearch(@RequestParam(defaultValue = "") String keyword) {
        List<SupplierDTO> list = supplierService.liveSearch(keyword);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupplierDTO>>> getAllSuppliers() {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.getAllSuppliers()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierDTO>> saveSupplier(@Valid @RequestBody SupplierDTO dto) {
        try {
            SupplierDTO saved = supplierService.saveSupplier(dto);
            return ResponseEntity.ok(ApiResponse.ok("Lưu nhà cung cấp thành công", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long id) {
        try {
            supplierService.deleteSupplier(id);
            return ResponseEntity.ok(ApiResponse.ok("Xóa nhà cung cấp thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
