package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "position", nullable = false, length = 100)
    private String position; // ROLE_ADMIN, ROLE_SALES, ROLE_WAREHOUSE

    @Column(name = "work_status", nullable = false, length = 30)
    private String workStatus; // ACTIVE, INACTIVE

    @Column(name = "balance", precision = 15, scale = 2)
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;
}
