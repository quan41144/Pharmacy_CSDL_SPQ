package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Long batchId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "batch_code", nullable = false, length = 100)
    private String batchCode;

    @Column(name = "mfg_date", nullable = false)
    private LocalDate mfgDate;

    @Column(name = "exp_date", nullable = false)
    private LocalDate expDate;
}
