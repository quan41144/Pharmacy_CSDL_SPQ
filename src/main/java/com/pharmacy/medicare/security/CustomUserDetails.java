package com.pharmacy.medicare.security;

import com.pharmacy.medicare.entity.Role;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.entity.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final BigDecimal balance;
    private final UserStatus status;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.fullName = user.getFullName();
        this.balance = user.getBalance();
        this.status = user.getStatus();
        this.authorities = user.getRoles().stream()
                .map(Role::getName)
                .map(r -> new SimpleGrantedAuthority(r.name()))
                .collect(Collectors.toSet());
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
