package com.pharmacy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "disposal_slip_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisposalSlipItem {
    @EmbeddedId
    private DisposalSlipItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("disposalSlipId")
    @JoinColumn(name = "disposal_slip_id")
    @JsonIgnore
    private DisposalSlip disposalSlip;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("batchId")
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;
}
