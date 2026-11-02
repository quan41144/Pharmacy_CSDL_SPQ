package com.pharmacy.medicare.dto.invoice;

import com.pharmacy.medicare.entity.InvoiceItem;

import java.math.BigDecimal;

public class InvoiceItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String unit;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;

    public InvoiceItemResponse() {
    }

    public static InvoiceItemResponse fromEntity(InvoiceItem item) {
        InvoiceItemResponse res = new InvoiceItemResponse();
        res.setId(item.getId());
        if (item.getProduct() != null) {
            res.setProductId(item.getProduct().getId());
            res.setProductName(item.getProduct().getName());
            res.setUnit(item.getProduct().getUnit() != null && !item.getProduct().getUnit().isBlank() 
                    ? item.getProduct().getUnit() : "Hộp");
        } else {
            res.setUnit("Hộp");
        }
        res.setQuantity(item.getQuantity());
        res.setUnitPrice(item.getUnitPrice());
        res.setSubTotal(item.getSubTotal());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }

    public void setSubTotal(BigDecimal subTotal) {
        this.subTotal = subTotal;
    }
}
