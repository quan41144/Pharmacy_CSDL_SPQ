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
public class InventoryId implements Serializable {
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "batch_id")
    private Long batchId;
}
