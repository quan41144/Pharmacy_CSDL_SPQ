package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.EmployeeDTO;
import com.pharmacy.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeDTO>>> getAllEmployees() {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getAllEmployees()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getEmployeeById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(employeeService.getEmployeeById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/admin-balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getAdminBalance() {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getAdminBalance()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeDTO>> saveEmployee(@RequestBody EmployeeDTO dto) {
        try {
            EmployeeDTO saved = employeeService.saveEmployee(dto);
            return ResponseEntity.ok(ApiResponse.ok("Lưu thông tin nhân viên thành công", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
