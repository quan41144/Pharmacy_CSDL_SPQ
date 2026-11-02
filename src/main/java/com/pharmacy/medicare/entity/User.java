package com.pharmacy.medicare.entity;

import com.pharmacy.medicare.entity.enums.RoleName;
import com.pharmacy.medicare.entity.enums.UserStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "username", length = 50, unique = true, nullable = false)
    private String username;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "position", length = 100)
    private String position = "none";

    @Column(name = "work_status", length = 30)
    private String workStatus = "INACTIVE";

    @Column(name = "start_date")
    private LocalDate startDate;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public User() {
    }

    public User(String username, String password, String fullName, BigDecimal balance, UserStatus status) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public boolean hasRole(RoleName roleName) {
        if (roles == null) return false;
        return roles.stream().anyMatch(r -> r.getName() == roleName);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getWorkStatus() {
        return workStatus;
    }

    public void setWorkStatus(String workStatus) {
        this.workStatus = workStatus;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}
