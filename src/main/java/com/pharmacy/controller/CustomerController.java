package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.CustomerDTO;
import com.pharmacy.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES', 'ADMIN')")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/live-search")
    public ResponseEntity<ApiResponse<List<CustomerDTO>>> liveSearch(@RequestParam(defaultValue = "") String keyword) {
        List<CustomerDTO> list = customerService.liveSearchCustomers(keyword);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerDTO>>> getAllCustomers() {
        return ResponseEntity.ok(ApiResponse.ok(customerService.getAllCustomers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCustomerById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(customerService.getCustomerById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDTO>> saveCustomer(@Valid @RequestBody CustomerDTO dto) {
        try {
            CustomerDTO saved = customerService.saveCustomer(dto);
            return ResponseEntity.ok(ApiResponse.ok("Lưu thông tin khách hàng thành công", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        try {
            customerService.deleteCustomer(id);
            return ResponseEntity.ok(ApiResponse.ok("Xóa khách hàng thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
