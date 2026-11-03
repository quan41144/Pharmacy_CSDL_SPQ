package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "import_slips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportSlip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "import_slip_id")
    private Long importSlipId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_staff_id", nullable = false)
    private Employee warehouseStaff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "import_date", nullable = false)
    private LocalDateTime importDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // PENDING, IMPORTED, CANCELLED

    @OneToMany(mappedBy = "importSlip", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ImportSlipItem> items = new ArrayList<>();
}
