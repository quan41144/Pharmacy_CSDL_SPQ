package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "disposal_slips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisposalSlip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disposal_slip_id")
    private Long disposalSlipId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_staff_id", nullable = false)
    private Employee warehouseStaff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "disposal_date")
    private LocalDateTime disposalDate;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // PENDING, DISPOSED, CANCELLED

    @OneToMany(mappedBy = "disposalSlip", fetch = FetchType.LAZY)
    @Builder.Default
    private List<DisposalSlipItem> items = new ArrayList<>();
}
