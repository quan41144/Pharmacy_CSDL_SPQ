package com.pharmacy.medicare.dto.user;

import com.pharmacy.medicare.entity.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    private UserStatus status;

    public UpdateStatusRequest() {
    }

    public UpdateStatusRequest(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
