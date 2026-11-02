package com.pharmacy.medicare.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "product_name", length = 200, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Transient
    private String description;

    @Column(name = "uses", columnDefinition = "TEXT")
    private String uses;

    @Column(name = "ingredients", columnDefinition = "TEXT")
    private String ingredients;

    @Column(name = "dosage_form", length = 100)
    private String dosageForm = "Viên nén";

    @Column(name = "unit", length = 50)
    private String unit = "Hộp";

    @Column(name = "import_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal importPrice = new BigDecimal("10000.00");

    @Column(name = "selling_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal sellingPrice = new BigDecimal("10000.00");

    @Column(name = "total_stock", nullable = false)
    private Integer totalStock = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Product() {
    }

    public Product(String name, Category category, String imageUrl, String description,
                   String uses, String ingredients, String unit, BigDecimal importPrice,
                   BigDecimal sellingPrice, Integer totalStock) {
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
        this.description = description;
        this.uses = uses;
        this.ingredients = ingredients;
        this.unit = (unit != null && !unit.isBlank()) ? unit : "Hộp";
        this.importPrice = importPrice != null ? importPrice : new BigDecimal("10000.00");
        this.sellingPrice = sellingPrice != null ? sellingPrice : this.importPrice;
        this.totalStock = totalStock != null ? totalStock : 0;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUses() {
        return uses;
    }

    public void setUses(String uses) {
        this.uses = uses;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = importPrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
