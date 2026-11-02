package com.pharmacy.medicare.controller;

import com.pharmacy.medicare.dto.ApiResponse;
import com.pharmacy.medicare.dto.notification.NotificationActionRequest;
import com.pharmacy.medicare.dto.notification.NotificationResponse;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.service.AdminApprovalService;
import com.pharmacy.medicare.service.AuthService;
import com.pharmacy.medicare.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AdminApprovalService adminApprovalService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService,
                                  AdminApprovalService adminApprovalService,
                                  AuthService authService) {
        this.notificationService = notificationService;
        this.adminApprovalService = adminApprovalService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications() {
        User user = authService.getCurrentAuthenticatedUser();
        List<NotificationResponse> list = notificationService.getMyNotifications(user);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        User user = authService.getCurrentAuthenticatedUser();
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        notificationService.markAsRead(id, user);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu thông báo là đã đọc", null));
    }

    @PostMapping("/{id}/action")
    public ResponseEntity<ApiResponse<String>> handleNotificationAction(
            @PathVariable Long id,
            @Valid @RequestBody NotificationActionRequest request) {
        User admin = authService.getCurrentAuthenticatedUser();
        String result = adminApprovalService.handleNotificationAction(id, request.getDecision(), admin);

        if (result.startsWith("Không thành công")) {
            return ResponseEntity.badRequest().body(ApiResponse.error(result, result));
        }
        return ResponseEntity.ok(ApiResponse.success(result, result));
    }
}
