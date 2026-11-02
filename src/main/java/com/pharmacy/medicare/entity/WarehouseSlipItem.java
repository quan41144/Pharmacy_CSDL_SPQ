package com.pharmacy.medicare.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "warehouse_slip_items")
public class WarehouseSlipItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_slip_items_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_slip_id", nullable = false)
    @JsonIgnore
    private WarehouseSlip warehouseSlip;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "sub_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal subTotal;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    public WarehouseSlipItem() {
    }

    public WarehouseSlipItem(WarehouseSlip warehouseSlip, Product product, Batch batch,
                             Integer quantity, BigDecimal unitPrice, String reason) {
        this.warehouseSlip = warehouseSlip;
        this.product = product;
        this.batch = batch;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subTotal = (unitPrice != null && quantity != null) ?
                unitPrice.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WarehouseSlip getWarehouseSlip() {
        return warehouseSlip;
    }

    public void setWarehouseSlip(WarehouseSlip warehouseSlip) {
        this.warehouseSlip = warehouseSlip;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
