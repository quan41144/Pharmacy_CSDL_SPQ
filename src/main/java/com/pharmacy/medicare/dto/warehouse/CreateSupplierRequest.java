package com.pharmacy.medicare.dto.warehouse;

import jakarta.validation.constraints.NotBlank;

public class CreateSupplierRequest {

    @NotBlank(message = "Tên nhà cung cấp không được để trống!")
    private String name;

    private String phone;
    private String representativeName;
    private String taxCode;
    private String address;

    public CreateSupplierRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRepresentativeName() {
        return representativeName;
    }

    public void setRepresentativeName(String representativeName) {
        this.representativeName = representativeName;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
