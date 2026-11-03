package com.pharmacy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {
    @EmbeddedId
    private InvoiceItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("invoiceId")
    @JoinColumn(name = "invoice_id")
    @JsonIgnore
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("batchId")
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "sub_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal subTotal;
}
