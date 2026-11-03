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
public class DisposalSlipItemId implements Serializable {
    @Column(name = "disposal_slip_id")
    private Long disposalSlipId;

    @Column(name = "batch_id")
    private Long batchId;
}
