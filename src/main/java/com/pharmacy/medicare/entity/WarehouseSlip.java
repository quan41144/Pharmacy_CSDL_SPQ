package com.pharmacy.medicare.entity;

import com.pharmacy.medicare.entity.enums.SlipStatus;
import com.pharmacy.medicare.entity.enums.SlipType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouse_slips")
public class WarehouseSlip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_slip_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private SlipType type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_staff_id", nullable = false)
    private User warehouseStaff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "disposal_date")
    private LocalDateTime disposalDate;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "result_state", length = 20)
    private String resultState = "PENDING"; // "true", "fail", "PENDING"

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SlipStatus status = SlipStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "warehouseSlip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<WarehouseSlipItem> items = new ArrayList<>();

    public WarehouseSlip() {
    }

    public WarehouseSlip(SlipType type, User warehouseStaff, Warehouse warehouse, Supplier supplier, BigDecimal totalAmount) {
        this.type = type;
        this.warehouseStaff = warehouseStaff;
        this.warehouse = warehouse;
        this.supplier = supplier;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.status = SlipStatus.PENDING;
        this.resultState = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SlipType getType() {
        return type;
    }

    public void setType(SlipType type) {
        this.type = type;
    }

    public User getWarehouseStaff() {
        return warehouseStaff;
    }

    public void setWarehouseStaff(User warehouseStaff) {
        this.warehouseStaff = warehouseStaff;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public LocalDateTime getDisposalDate() {
        return disposalDate;
    }

    public void setDisposalDate(LocalDateTime disposalDate) {
        this.disposalDate = disposalDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getResultState() {
        return resultState;
    }

    public void setResultState(String resultState) {
        this.resultState = resultState;
    }

    public SlipStatus getStatus() {
        return status;
    }

    public void setStatus(SlipStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<WarehouseSlipItem> getItems() {
        return items;
    }

    public void setItems(List<WarehouseSlipItem> items) {
        this.items = items;
    }

    public void addItem(WarehouseSlipItem item) {
        items.add(item);
        item.setWarehouseSlip(this);
    }

    // Helper compatibility getters for single-item slips
    public Product getProduct() {
        return items != null && !items.isEmpty() ? items.get(0).getProduct() : null;
    }

    public void setProduct(Product product) {
        // Compatibility
    }

    public Batch getBatch() {
        return items != null && !items.isEmpty() ? items.get(0).getBatch() : null;
    }

    public void setBatch(Batch batch) {
        // Compatibility
    }

    public Integer getQuantity() {
        return items != null && !items.isEmpty() ? items.get(0).getQuantity() : 0;
    }

    public void setQuantity(Integer quantity) {
        // Compatibility
    }

    public BigDecimal getUnitPrice() {
        return items != null && !items.isEmpty() ? items.get(0).getUnitPrice() : BigDecimal.ZERO;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        // Compatibility
    }

    public String getReason() {
        return items != null && !items.isEmpty() ? items.get(0).getReason() : null;
    }

    public void setReason(String reason) {
        if (items != null && !items.isEmpty()) {
            items.get(0).setReason(reason);
        }
    }
}
