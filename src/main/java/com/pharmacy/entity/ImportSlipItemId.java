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
public class ImportSlipItemId implements Serializable {
    @Column(name = "import_slip_id")
    private Long importSlipId;

    @Column(name = "batch_id")
    private Long batchId;
}
