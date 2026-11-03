package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.LoginRequest;
import com.pharmacy.dto.LoginResponse;
import com.pharmacy.dto.RefreshTokenRequest;
import com.pharmacy.entity.Employee;
import com.pharmacy.repository.EmployeeRepository;
import com.pharmacy.service.AuthService;
import com.pharmacy.service.EmployeeService;
import com.pharmacy.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            LoginResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(ApiResponse.ok("Cấp mới token thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }

        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        LoginResponse resp = LoginResponse.builder()
                .employeeId(employee.getEmployeeId())
                .username(employee.getUsername())
                .fullName(employee.getFullName())
                .position(employee.getPosition())
                .warehouseId(employee.getWarehouse() != null ? employee.getWarehouse().getWarehouseId() : null)
                .warehouseName(employee.getWarehouse() != null ? employee.getWarehouse().getWarehouseName() : null)
                .balance(employee.getBalance())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(resp));
    }
}
