package com.pharmacy.medicare.entity;

import com.pharmacy.medicare.entity.enums.BatchStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "batches")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "mfg_date", nullable = false)
    private LocalDate mfgDate;

    @Column(name = "exp_date", nullable = false)
    private LocalDate expDate;

    @Column(name = "quantify", nullable = false)
    private Integer quantity;

    @Column(name = "import_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal importPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private BatchStatus status = BatchStatus.WAITING_FOR_PRICE;

    @Column(name = "is_damaged", nullable = false)
    private Boolean isDamaged = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Batch() {
    }

    public Batch(Product product, LocalDate mfgDate, LocalDate expDate,
                 Integer quantity, BigDecimal importPrice, BatchStatus status, Boolean isDamaged) {
        this.product = product;
        this.mfgDate = mfgDate;
        this.expDate = expDate;
        this.quantity = quantity;
        this.importPrice = importPrice;
        this.status = status != null ? status : BatchStatus.WAITING_FOR_PRICE;
        this.isDamaged = isDamaged != null ? isDamaged : false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Batch(Product product, Warehouse warehouse, LocalDate mfgDate, LocalDate expDate,
                 Integer quantity, BigDecimal importPrice, BatchStatus status, Boolean isDamaged) {
        this.product = product;
        this.warehouse = warehouse;
        this.mfgDate = mfgDate;
        this.expDate = expDate;
        this.quantity = quantity;
        this.importPrice = importPrice;
        this.status = status != null ? status : BatchStatus.WAITING_FOR_PRICE;
        this.isDamaged = isDamaged != null ? isDamaged : false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expDate != null && expDate.isBefore(LocalDate.now());
    }

    public boolean isEligibleForDisposal() {
        return isDamaged || (expDate != null && !expDate.isAfter(LocalDate.now()));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = importPrice;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public Boolean getIsDamaged() {
        return isDamaged;
    }

    public void setIsDamaged(Boolean isDamaged) {
        this.isDamaged = isDamaged;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
