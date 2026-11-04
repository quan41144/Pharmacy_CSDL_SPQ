package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.EmployeeDTO;
import com.pharmacy.dto.UpdateProfileRequest;
import com.pharmacy.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // ================= ADMIN: QUẢN LÝ NHÂN SỰ & TÀI KHOẢN =================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<EmployeeDTO>>> getAllEmployees() {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getAllEmployees()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getEmployeeById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(employeeService.getEmployeeById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/admin-balance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BigDecimal>> getAdminBalance() {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getAdminBalance()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeDTO>> saveEmployee(@RequestBody EmployeeDTO dto) {
        try {
            EmployeeDTO saved = employeeService.saveEmployee(dto);
            return ResponseEntity.ok(ApiResponse.ok("Lưu thông tin nhân viên thành công", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeDTO>> toggleEmployeeStatus(@PathVariable Long id) {
        try {
            EmployeeDTO updated = employeeService.toggleEmployeeStatus(id);
            return ResponseEntity.ok(ApiResponse.ok("Đổi trạng thái nhân viên thành công", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ================= TẤT CẢ TÀI KHOẢN: XEM & SỬA THÔNG TIN CÁ NHÂN =================

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getCurrentProfile() {
        try {
            EmployeeDTO profile = employeeService.getCurrentUserProfile();
            return ResponseEntity.ok(ApiResponse.ok(profile));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EmployeeDTO>> updateCurrentProfile(@RequestBody UpdateProfileRequest request) {
        try {
            EmployeeDTO updated = employeeService.updateCurrentUserProfile(request);
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin cá nhân thành công", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
