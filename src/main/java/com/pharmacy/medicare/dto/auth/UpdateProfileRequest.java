package com.pharmacy.medicare.dto.auth;

import java.time.LocalDate;

public class UpdateProfileRequest {

    private String fullName;
    private LocalDate dateOfBirth;
    private String phone;
    private String address;
    private String gender;
    private String password;

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String fullName, String password) {
        this.fullName = fullName;
        this.password = password;
    }

    public UpdateProfileRequest(String fullName, LocalDate dateOfBirth, String phone, String address, String gender) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
        this.address = address;
        this.gender = gender;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
