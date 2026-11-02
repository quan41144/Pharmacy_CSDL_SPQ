package com.pharmacy.medicare.controller;

import com.pharmacy.medicare.dto.ApiResponse;
import com.pharmacy.medicare.dto.auth.UpdateProfileRequest;
import com.pharmacy.medicare.dto.user.ChangePasswordRequest;
import com.pharmacy.medicare.dto.user.UserResponse;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.service.AuthService;
import com.pharmacy.medicare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/users", "/api/users"})
public class UserProfileController {

    private final UserService userService;
    private final AuthService authService;

    public UserProfileController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ cá nhân thành công", UserResponse.fromEntity(currentUser)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@RequestBody UpdateProfileRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        UserResponse response = userService.updateProfile(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ cá nhân thành công!", response));
    }

    @RequestMapping(value = "/change-password", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        userService.changePassword(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công! Vui lòng sử dụng mật khẩu mới cho các lần đăng nhập tiếp theo.", null));
    }
}
