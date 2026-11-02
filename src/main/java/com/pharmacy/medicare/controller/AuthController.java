package com.pharmacy.medicare.controller;

import com.pharmacy.medicare.dto.ApiResponse;
import com.pharmacy.medicare.dto.auth.AuthResponse;
import com.pharmacy.medicare.dto.auth.LoginRequest;
import com.pharmacy.medicare.dto.auth.RefreshTokenRequest;
import com.pharmacy.medicare.dto.auth.RegisterRequest;
import com.pharmacy.medicare.dto.user.AdjustBalanceRequest;
import com.pharmacy.medicare.dto.user.UserResponse;
import com.pharmacy.medicare.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký tài khoản thành công! Quyền mặc định: ROLE_CUSTOMER", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse response = authService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài khoản thành công", response));
    }

    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<UserResponse>> topUpBalance(@Valid @RequestBody AdjustBalanceRequest request) {
        UserResponse response = authService.topUpMyBalance(request.getAmount());
        return ResponseEntity.ok(ApiResponse.success("Nạp tiền vào tài khoản thành công!", response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@RequestBody com.pharmacy.medicare.dto.auth.UpdateProfileRequest request) {
        UserResponse response = authService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin cá nhân thành công!", response));
    }
}
