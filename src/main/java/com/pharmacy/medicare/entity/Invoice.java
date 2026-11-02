package com.pharmacy.medicare.entity;

import com.pharmacy.medicare.entity.enums.InvoiceStatus;
import com.pharmacy.medicare.entity.enums.PaymentMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sales_staff_id", nullable = false)
    private User salesStaff;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30, nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "result_state", length = 20)
    private String resultState = "pending"; // "true", "fail", "pending"

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceItem> items = new ArrayList<>();

    public Invoice() {
    }

    public Invoice(User customer, User salesStaff, BigDecimal totalAmount,
                   PaymentMethod paymentMethod, int expirationMinutes) {
        this.customer = customer;
        this.salesStaff = salesStaff;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.status = InvoiceStatus.UNPAID;
        this.resultState = "pending";
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusMinutes(expirationMinutes > 0 ? expirationMinutes : 10);
    }

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public User getSalesStaff() {
        return salesStaff;
    }

    public void setSalesStaff(User salesStaff) {
        this.salesStaff = salesStaff;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getResultState() {
        return resultState;
    }

    public void setResultState(String resultState) {
        this.resultState = resultState;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
