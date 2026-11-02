package com.pharmacy.medicare.dto.product;

import com.pharmacy.medicare.entity.User;

public class SalesStaffResponse {

    private Long id;
    private String fullName;
    private String username;

    public SalesStaffResponse() {
    }

    public static SalesStaffResponse fromEntity(User user) {
        SalesStaffResponse res = new SalesStaffResponse();
        res.setId(user.getId());
        res.setFullName(user.getFullName());
        res.setUsername(user.getUsername());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
