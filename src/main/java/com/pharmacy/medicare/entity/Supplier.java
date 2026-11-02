package com.pharmacy.medicare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long id;

    @Column(name = "supplier_name", length = 150, nullable = false)
    private String name;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "representative_name", length = 100)
    private String representativeName;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Supplier() {
    }

    public Supplier(String name, String phone, String representativeName, String taxCode, String address) {
        this.name = name;
        this.phone = phone;
        this.representativeName = representativeName;
        this.taxCode = taxCode;
        this.address = address;
        this.createdAt = LocalDateTime.now();
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
