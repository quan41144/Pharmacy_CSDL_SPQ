package com.pharmacy.medicare.dto.product;

import com.pharmacy.medicare.entity.Batch;
import com.pharmacy.medicare.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProductResponse {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String imageUrl;
    private String description;
    private String uses;
    private String ingredients;
    private String dosageForm;
    private String unit;
    private BigDecimal importPrice;
    private BigDecimal sellingPrice;
    private Integer totalStock;
    private Boolean isActive;
    private LocalDate mfgDate;
    private LocalDate expDate;
    private LocalDateTime createdAt;

    public ProductResponse() {
    }

    public static ProductResponse fromEntity(Product p) {
        return fromEntity(p, null);
    }

    public static ProductResponse fromEntity(Product p, Batch activeBatch) {
        ProductResponse res = new ProductResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        if (p.getCategory() != null) {
            res.setCategoryId(p.getCategory().getId());
            res.setCategoryName(p.getCategory().getName());
        }
        res.setImageUrl(p.getImageUrl());
        res.setDescription(p.getDescription());
        res.setUses(p.getUses());
        res.setIngredients(p.getIngredients());
        res.setDosageForm(p.getDosageForm() != null ? p.getDosageForm() : "Viên nén");
        res.setUnit(p.getUnit());
        res.setImportPrice(p.getImportPrice());
        res.setSellingPrice(p.getSellingPrice());
        res.setTotalStock(p.getTotalStock());
        res.setIsActive(p.getIsActive() != null ? p.getIsActive() : true);
        res.setCreatedAt(p.getCreatedAt());

        if (activeBatch != null) {
            res.setMfgDate(activeBatch.getMfgDate());
            res.setExpDate(activeBatch.getExpDate());
        }

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDate getMfgDate() {
        return mfgDate;
    }

    public void setMfgDate(LocalDate mfgDate) {
        this.mfgDate = mfgDate;
    }

    public LocalDate getExpDate() {
        return expDate;
    }

    public void setExpDate(LocalDate expDate) {
        this.expDate = expDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
