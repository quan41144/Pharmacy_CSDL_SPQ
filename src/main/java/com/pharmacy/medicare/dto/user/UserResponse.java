package com.pharmacy.medicare.dto.user;

import com.pharmacy.medicare.entity.Role;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.entity.enums.RoleName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private BigDecimal balance;
    private String status;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDate dateOfBirth;
    private String phone;
    private String address;
    private String gender;
    private String position;
    private String workStatus;
    private LocalDate startDate;

    public UserResponse() {
    }

    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setBalance(user.getBalance());
        response.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        response.setCreatedAt(user.getCreatedAt());
        response.setDateOfBirth(user.getDateOfBirth());
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setGender(user.getGender());
        response.setWorkStatus(user.getWorkStatus() != null ? user.getWorkStatus() : "ACTIVE");
        response.setStartDate(user.getStartDate());

        if (user.getRoles() != null) {
            response.setRoles(user.getRoles().stream()
                    .map(Role::getName)
                    .map(Enum::name)
                    .collect(Collectors.toSet()));
        }

        // Tự động định vị Position nếu chưa có
        if (user.getPosition() != null && !user.getPosition().isBlank()) {
            response.setPosition(user.getPosition());
        } else if (user.hasRole(RoleName.ROLE_ADMIN)) {
            response.setPosition("Quản trị viên");
        } else if (user.hasRole(RoleName.ROLE_SALES)) {
            response.setPosition("Nhân viên bán hàng");
        } else if (user.hasRole(RoleName.ROLE_WAREHOUSE)) {
            response.setPosition("Nhân viên kho");
        } else {
            response.setPosition("Khách hàng");
        }

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getWorkStatus() {
        return workStatus;
    }

    public void setWorkStatus(String workStatus) {
        this.workStatus = workStatus;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}
