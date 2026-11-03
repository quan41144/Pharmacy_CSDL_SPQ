package com.pharmacy.service;

import com.pharmacy.config.JwtTokenProvider;
import com.pharmacy.dto.LoginRequest;
import com.pharmacy.dto.LoginResponse;
import com.pharmacy.dto.RefreshTokenRequest;
import com.pharmacy.entity.Employee;
import com.pharmacy.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        if (!"ACTIVE".equalsIgnoreCase(employee.getWorkStatus())) {
            throw new RuntimeException("Tài khoản nhân viên đã ngừng làm việc (INACTIVE). Không được phép đăng nhập.");
        }

        String role = employee.getPosition();
        String accessToken = tokenProvider.generateAccessToken(employee.getUsername(), role, employee.getEmployeeId());
        String refreshToken = tokenProvider.generateRefreshToken(employee.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .employeeId(employee.getEmployeeId())
                .username(employee.getUsername())
                .fullName(employee.getFullName())
                .position(employee.getPosition())
                .warehouseId(employee.getWarehouse() != null ? employee.getWarehouse().getWarehouseId() : null)
                .warehouseName(employee.getWarehouse() != null ? employee.getWarehouse().getWarehouseName() : null)
                .balance(employee.getBalance())
                .build();
    }

    @Transactional(readOnly = true)
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên"));

        if (!"ACTIVE".equalsIgnoreCase(employee.getWorkStatus())) {
            throw new RuntimeException("Tài khoản nhân viên đã ngừng làm việc");
        }

        String newAccessToken = tokenProvider.generateAccessToken(employee.getUsername(), employee.getPosition(), employee.getEmployeeId());
        String newRefreshToken = tokenProvider.generateRefreshToken(employee.getUsername());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .employeeId(employee.getEmployeeId())
                .username(employee.getUsername())
                .fullName(employee.getFullName())
                .position(employee.getPosition())
                .warehouseId(employee.getWarehouse() != null ? employee.getWarehouse().getWarehouseId() : null)
                .warehouseName(employee.getWarehouse() != null ? employee.getWarehouse().getWarehouseName() : null)
                .balance(employee.getBalance())
                .build();
    }
}
