package com.pharmacy.medicare.service;

import com.pharmacy.medicare.dto.user.CreateUserRequest;
import com.pharmacy.medicare.dto.user.UserResponse;
import com.pharmacy.medicare.entity.Role;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.entity.enums.RoleName;
import com.pharmacy.medicare.entity.enums.UserStatus;
import com.pharmacy.medicare.repository.RoleRepository;
import com.pharmacy.medicare.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(String roleFilter, String statusFilter, String keyword) {
        List<User> users;
        if (StringUtils.hasText(keyword)) {
            users = userRepository.searchUsers(keyword.trim());
        } else {
            users = userRepository.findAll();
        }

        // Lọc theo Role nếu có
        if (StringUtils.hasText(roleFilter) && !"ALL".equalsIgnoreCase(roleFilter)) {
            try {
                RoleName targetRole = RoleName.valueOf(roleFilter.trim());
                users = users.stream()
                        .filter(u -> u.hasRole(targetRole))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Lọc theo Trạng thái nếu có
        if (StringUtils.hasText(statusFilter) && !"ALL".equalsIgnoreCase(statusFilter)) {
            try {
                UserStatus targetStatus = UserStatus.valueOf(statusFilter.trim());
                users = users.stream()
                        .filter(u -> u.getStatus() == targetStatus)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return users.stream()
                .sorted(Comparator.comparing(User::getId))
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy người dùng với ID: " + id));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' đã tồn tại!");
        }

        Role primaryRole = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("Role không hợp lệ: " + request.getRole()));

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("ROLE_CUSTOMER chưa được khởi tạo"));

        User newUser = new User(
                request.getUsername().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO,
                UserStatus.ACTIVE
        );

        Set<Role> roles = new HashSet<>();
        roles.add(primaryRole);
        // Luôn kế thừa quyền ROLE_CUSTOMER để mua sắm
        roles.add(customerRole);
        newUser.setRoles(roles);

        if (request.getRole() == RoleName.ROLE_CUSTOMER) {
            newUser.setPosition("none");
            newUser.setWorkStatus("INACTIVE");
            newUser.setStartDate(null);
        } else {
            newUser.setPosition(switch (request.getRole()) {
                case ROLE_ADMIN -> "Quản trị viên";
                case ROLE_SALES -> "Nhân viên bán hàng";
                case ROLE_WAREHOUSE -> "Nhân viên kho";
                default -> "Nhân viên";
            });
            newUser.setWorkStatus("ACTIVE");
            newUser.setStartDate(LocalDate.now());
        }

        User saved = userRepository.save(newUser);
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse updateUserRole(Long targetUserId, RoleName newRole, String currentAdminUsername) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy người dùng với ID: " + targetUserId));

        // RÀNG BUỘC BẢO MẬT: Tuyệt đối không can thiệp vào tài khoản ROLE_ADMIN khác
        if (targetUser.hasRole(RoleName.ROLE_ADMIN) && !targetUser.getUsername().equals(currentAdminUsername)) {
            throw new AccessDeniedException("Quy tắc bảo mật: Tuyệt đối không được sửa đổi quyền của tài khoản Admin khác!");
        }

        Role roleToAssign = roleRepository.findByName(newRole)
                .orElseThrow(() -> new IllegalArgumentException("Role không tồn tại: " + newRole));

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("ROLE_CUSTOMER chưa được khởi tạo"));

        Set<Role> roles = new HashSet<>();
        roles.add(roleToAssign);
        // Mọi nhân viên và admin đều kế thừa quyền ROLE_CUSTOMER
        roles.add(customerRole);

        targetUser.setRoles(roles);

        if (newRole == RoleName.ROLE_SALES || newRole == RoleName.ROLE_WAREHOUSE || newRole == RoleName.ROLE_ADMIN) {
            targetUser.setStartDate(java.time.LocalDate.now());
            targetUser.setWorkStatus("ACTIVE");
            targetUser.setPosition(switch (newRole) {
                case ROLE_ADMIN -> "Quản trị viên";
                case ROLE_SALES -> "Nhân viên bán hàng";
                case ROLE_WAREHOUSE -> "Nhân viên kho";
                default -> "Nhân viên";
            });
        } else {
            targetUser.setPosition("none");
            targetUser.setWorkStatus("INACTIVE");
            targetUser.setStartDate(null);
        }

        User saved = userRepository.save(targetUser);
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse updateUserStatus(Long targetUserId, UserStatus newStatus, String currentAdminUsername) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy người dùng với ID: " + targetUserId));

        // RÀNG BUỘC BẢO MẬT: Tuyệt đối không được khóa tài khoản Admin
        if (targetUser.hasRole(RoleName.ROLE_ADMIN)) {
            throw new AccessDeniedException("Quy tắc bảo mật: Tuyệt đối không được khóa hoặc thay đổi trạng thái của tài khoản Admin!");
        }

        targetUser.setStatus(newStatus);
        User saved = userRepository.save(targetUser);
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse adjustBalance(Long targetUserId, BigDecimal amount, String note) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy người dùng với ID: " + targetUserId));

        if (amount == null) {
            throw new IllegalArgumentException("Số tiền điều chỉnh không được để trống");
        }

        BigDecimal updatedBalance = targetUser.getBalance().add(amount);
        if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số dư tài khoản sau khi điều chỉnh không thể âm (Hiện tại: "
                    + targetUser.getBalance() + " VND, Điều chỉnh: " + amount + " VND)");
        }

        targetUser.setBalance(updatedBalance);
        User saved = userRepository.save(targetUser);
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteUser(Long targetUserId, String currentAdminUsername) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy người dùng với ID: " + targetUserId));

        // RÀNG BUỘC BẢO MẬT: Tuyệt đối không được xóa tài khoản Admin
        if (targetUser.hasRole(RoleName.ROLE_ADMIN)) {
            throw new AccessDeniedException("Quy tắc bảo mật: Tuyệt đối không được xóa tài khoản Admin!");
        }

        userRepository.delete(targetUser);
    }

    @Transactional
    public void changePassword(User user, com.pharmacy.medicare.dto.user.ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác!");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Xác nhận mật khẩu mới không trùng khớp!");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateProfile(User user, com.pharmacy.medicare.dto.auth.UpdateProfileRequest request) {
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender().trim());
        }

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }
}
