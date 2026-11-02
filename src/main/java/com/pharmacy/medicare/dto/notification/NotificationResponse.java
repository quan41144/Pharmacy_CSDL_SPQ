package com.pharmacy.medicare.dto.notification;

import com.pharmacy.medicare.entity.Notification;

import java.time.LocalDateTime;

public class NotificationResponse {

    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String type;
    private Long referenceId;
    private String message;
    private Boolean isRead;
    private String actionStatus;
    private LocalDateTime createdAt;

    public NotificationResponse() {
    }

    public static NotificationResponse fromEntity(Notification n) {
        NotificationResponse res = new NotificationResponse();
        res.setId(n.getId());
        if (n.getSender() != null) {
            res.setSenderId(n.getSender().getId());
            res.setSenderName(n.getSender().getFullName());
        }
        if (n.getReceiver() != null) {
            res.setReceiverId(n.getReceiver().getId());
            res.setReceiverName(n.getReceiver().getFullName());
        }
        res.setType(n.getType() != null ? n.getType().name() : null);
        res.setReferenceId(n.getReferenceId());
        res.setMessage(n.getMessage());
        res.setIsRead(n.getIsRead());
        res.setActionStatus(n.getActionStatus() != null ? n.getActionStatus().name() : null);
        res.setCreatedAt(n.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public String getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(String actionStatus) {
        this.actionStatus = actionStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
