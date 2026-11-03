package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name", nullable = false, length = 150)
    private String supplierName;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "representative_name", length = 100)
    private String representativeName;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;
}
