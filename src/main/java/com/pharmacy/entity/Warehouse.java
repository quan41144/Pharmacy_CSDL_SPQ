package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name", nullable = false, length = 150)
    private String warehouseName;

    @Column(name = "warehouse_address", nullable = false, length = 255)
    private String warehouseAddress;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // 'ACTIVE', 'INACTIVE'
}
