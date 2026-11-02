package com.pharmacy.medicare.dto.product;

import jakarta.validation.constraints.NotBlank;

public class CreateCategoryRequest {

    @NotBlank(message = "Tên nhóm thuốc không được để trống!")
    private String name;

    private String description;

    public CreateCategoryRequest() {
    }

    public CreateCategoryRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
