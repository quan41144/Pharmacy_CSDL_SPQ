package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "dosage", length = 100)
    private String dosage;

    @Column(name = "uses", columnDefinition = "text")
    private String uses;

    @Column(name = "dosage_form", length = 100)
    private String dosageForm;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Column(name = "selling_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;
}
