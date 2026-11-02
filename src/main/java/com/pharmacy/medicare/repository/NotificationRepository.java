package com.pharmacy.medicare.repository;

import com.pharmacy.medicare.entity.Notification;
import com.pharmacy.medicare.entity.enums.ActionStatus;
import com.pharmacy.medicare.entity.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    List<Notification> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    List<Notification> findByReceiverIdAndActionStatusOrderByCreatedAtDesc(Long receiverId, ActionStatus actionStatus);

    List<Notification> findByTypeAndReferenceId(NotificationType type, Long referenceId);
}
