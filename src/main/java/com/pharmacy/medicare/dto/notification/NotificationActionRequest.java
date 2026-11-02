package com.pharmacy.medicare.dto.notification;

import jakarta.validation.constraints.NotBlank;

public class NotificationActionRequest {

    @NotBlank(message = "Quyết định không được để trống (ACCEPT hoặc REJECT)")
    private String decision; // "ACCEPT" hoặc "REJECT"

    public NotificationActionRequest() {
    }

    public NotificationActionRequest(String decision) {
        this.decision = decision;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }
}
