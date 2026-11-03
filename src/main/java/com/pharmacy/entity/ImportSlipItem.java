package com.pharmacy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "import_slip_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportSlipItem {
    @EmbeddedId
    private ImportSlipItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("importSlipId")
    @JoinColumn(name = "import_slip_id")
    @JsonIgnore
    private ImportSlip importSlip;

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
