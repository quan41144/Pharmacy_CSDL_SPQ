package com.pharmacy.medicare.controller;

import com.pharmacy.medicare.dto.ApiResponse;
import com.pharmacy.medicare.dto.user.*;
import com.pharmacy.medicare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        List<UserResponse> list = userService.getAllUsers(role, status, keyword);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo tài khoản mới thành công", created));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request,
            Authentication authentication) {
        UserResponse updated = userService.updateUserRole(id, request.getRole(), authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phân quyền tài khoản thành công", updated));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        UserResponse updated = userService.updateUserStatus(id, request.getStatus(), authentication.getName());
        String msg = request.getStatus().name().equals("LOCKED") ? "Đã khóa tài khoản thành công" : "Đã mở khóa tài khoản thành công";
        return ResponseEntity.ok(ApiResponse.success(msg, updated));
    }

    @PostMapping("/{id}/balance")
    public ResponseEntity<ApiResponse<UserResponse>> adjustUserBalance(
            @PathVariable Long id,
            @Valid @RequestBody AdjustBalanceRequest request) {
        UserResponse updated = userService.adjustBalance(id, request.getAmount(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success("Điều chỉnh số dư thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {
        userService.deleteUser(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa tài khoản thành công", null));
    }
}
