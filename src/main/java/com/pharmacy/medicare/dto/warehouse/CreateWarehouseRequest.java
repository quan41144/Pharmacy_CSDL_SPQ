package com.pharmacy.medicare.dto.warehouse;

import jakarta.validation.constraints.NotBlank;

public class CreateWarehouseRequest {

    @NotBlank(message = "Tên kho không được để trống!")
    private String warehouseName;

    @NotBlank(message = "Địa chỉ kho không được để trống!")
    private String warehouseAddress;

    public CreateWarehouseRequest() {
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseAddress() {
        return warehouseAddress;
    }

    public void setWarehouseAddress(String warehouseAddress) {
        this.warehouseAddress = warehouseAddress;
    }
}
