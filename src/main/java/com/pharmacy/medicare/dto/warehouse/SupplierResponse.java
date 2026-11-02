package com.pharmacy.medicare.dto.warehouse;

import com.pharmacy.medicare.entity.Supplier;
import java.time.LocalDateTime;

public class SupplierResponse {

    private Long id;
    private String name;
    private String phone;
    private String representativeName;
    private String taxCode;
    private String address;
    private LocalDateTime createdAt;

    public SupplierResponse() {
    }

    public static SupplierResponse fromEntity(Supplier s) {
        SupplierResponse res = new SupplierResponse();
        res.setId(s.getId());
        res.setName(s.getName());
        res.setPhone(s.getPhone());
        res.setRepresentativeName(s.getRepresentativeName());
        res.setTaxCode(s.getTaxCode());
        res.setAddress(s.getAddress());
        res.setCreatedAt(s.getCreatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
