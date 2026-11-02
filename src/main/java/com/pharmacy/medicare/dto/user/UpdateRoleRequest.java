package com.pharmacy.medicare.dto.user;

import com.pharmacy.medicare.entity.enums.RoleName;
import jakarta.validation.constraints.NotNull;

public class UpdateRoleRequest {

    @NotNull(message = "Role mới không được để trống")
    private RoleName role;

    public UpdateRoleRequest() {
    }

    public UpdateRoleRequest(RoleName role) {
        this.role = role;
    }

    public RoleName getRole() {
        return role;
    }

    public void setRole(RoleName role) {
        this.role = role;
    }
}
