package com.pharmacy.medicare.service;

import com.pharmacy.medicare.dto.auth.AuthResponse;
import com.pharmacy.medicare.dto.auth.LoginRequest;
import com.pharmacy.medicare.dto.auth.RefreshTokenRequest;
import com.pharmacy.medicare.dto.auth.RegisterRequest;
import com.pharmacy.medicare.dto.user.UserResponse;
import com.pharmacy.medicare.entity.Role;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.entity.enums.RoleName;
import com.pharmacy.medicare.entity.enums.UserStatus;
import com.pharmacy.medicare.repository.RoleRepository;
import com.pharmacy.medicare.repository.UserRepository;
import com.pharmacy.medicare.security.CustomUserDetails;
import com.pharmacy.medicare.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new LockedException("Tài khoản của bạn đã bị khóa bởi Quản trị viên");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getBalance(),
                roleNames,
                user.getStatus().name()
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + request.getUsername() + "' đã tồn tại trên hệ thống!");
        }

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Role ROLE_CUSTOMER chưa được khởi tạo!"));

        User newUser = new User(
                request.getUsername().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                BigDecimal.ZERO, // Mặc định số dư là 0
                UserStatus.ACTIVE
        );
        newUser.setRoles(new HashSet<>(Collections.singletonList(customerRole)));
        newUser.setPosition("none");
        newUser.setWorkStatus("INACTIVE");
        newUser.setStartDate(null);
        User savedUser = userRepository.save(newUser);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        Set<String> roleNames = Collections.singleton(RoleName.ROLE_CUSTOMER.name());

        return new AuthResponse(
                accessToken,
                refreshToken,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getFullName(),
                savedUser.getBalance(),
                roleNames,
                savedUser.getStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (jwtService.isTokenExpired(token)) {
            throw new BadCredentialsException("Refresh Token đã hết hạn, vui lòng đăng nhập lại!");
        }

        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Người dùng không tồn tại"));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new LockedException("Tài khoản đã bị khóa!");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return new AuthResponse(
                newAccessToken,
                token,
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getBalance(),
                roleNames,
                user.getStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BadCredentialsException("Chưa đăng nhập");
        }
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Không tìm thấy thông tin tài khoản hiện tại"));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        User user = getCurrentAuthenticatedUser();
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse topUpMyBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        User user = getCurrentAuthenticatedUser();
        user.setBalance(user.getBalance().add(amount));
        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse updateProfile(com.pharmacy.medicare.dto.auth.UpdateProfileRequest request) {
        User user = getCurrentAuthenticatedUser();
        if (org.springframework.util.StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }
        if (org.springframework.util.StringUtils.hasText(request.getPassword())) {
            if (request.getPassword().trim().length() < 5) {
                throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 5 ký tự!");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }
        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }
}
