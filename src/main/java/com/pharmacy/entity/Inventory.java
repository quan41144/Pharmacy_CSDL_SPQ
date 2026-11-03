package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {
    @EmbeddedId
    private InventoryId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("warehouseId")
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("batchId")
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
