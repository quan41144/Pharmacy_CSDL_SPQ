package com.pharmacy.medicare.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateProductRequest {

    @NotBlank(message = "Tên thuốc là bắt buộc!")
    private String name;

    @NotNull(message = "Nhóm thuốc là bắt buộc!")
    private Long categoryId;

    @NotBlank(message = "Mô tả thuốc là bắt buộc!")
    private String description;

    @NotBlank(message = "Công dụng thuốc là bắt buộc!")
    private String uses;

    @NotBlank(message = "Thành phần thuốc là bắt buộc!")
    private String ingredients;

    private String dosageForm = "Viên nén";

    @NotBlank(message = "Đơn vị tính là bắt buộc!")
    private String unit;

    @NotBlank(message = "Hình ảnh thuốc là bắt buộc!")
    private String imageUrl;

    @NotNull(message = "Giá nhập thuốc là bắt buộc!")
    @DecimalMin(value = "0.0", message = "Giá nhập phải lớn hơn hoặc bằng 0")
    private BigDecimal importPrice = new BigDecimal("10000.00");

    @NotNull(message = "Giá bán thuốc là bắt buộc!")
    @DecimalMin(value = "0.0", message = "Giá bán phải lớn hơn hoặc bằng 0")
    private BigDecimal sellingPrice;

    public CreateProductRequest() {
    }

    public CreateProductRequest(String name, Long categoryId, String description, String uses,
                                String ingredients, String unit, String imageUrl,
                                BigDecimal importPrice, BigDecimal sellingPrice) {
        this.name = name;
        this.categoryId = categoryId;
        this.description = description;
        this.uses = uses;
        this.ingredients = ingredients;
        this.unit = unit;
        this.imageUrl = imageUrl;
        this.importPrice = importPrice;
        this.sellingPrice = sellingPrice;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }
}
