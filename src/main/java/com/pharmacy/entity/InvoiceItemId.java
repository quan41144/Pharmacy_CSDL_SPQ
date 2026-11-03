package com.pharmacy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class InvoiceItemId implements Serializable {
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "batch_id")
    private Long batchId;
}
