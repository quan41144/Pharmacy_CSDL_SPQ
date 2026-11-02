package com.pharmacy.medicare.service;

import com.pharmacy.medicare.dto.notification.NotificationResponse;
import com.pharmacy.medicare.entity.Notification;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.repository.NotificationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(User user) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thông báo ID: " + notificationId));

        if (!notification.getReceiver().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền đánh dấu thông báo này");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
}
