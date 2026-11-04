package com.pharmacy.service;

import com.pharmacy.dto.EmployeeDTO;
import com.pharmacy.dto.UpdateProfileRequest;
import com.pharmacy.entity.Employee;
import com.pharmacy.entity.Warehouse;
import com.pharmacy.repository.EmployeeRepository;
import com.pharmacy.repository.WarehouseRepository;
import com.pharmacy.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final WarehouseRepository warehouseRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findAllByOrderByEmployeeIdAsc().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeById(Long id) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        return toDTO(e);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAdminBalance() {
        List<Employee> admins = employeeRepository.findByPositionAndWorkStatus("ROLE_ADMIN", "ACTIVE");
        if (admins.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return admins.get(0).getBalance() != null ? admins.get(0).getBalance() : BigDecimal.ZERO;
    }

    @Transactional
    public EmployeeDTO saveEmployee(EmployeeDTO dto) {
        Employee employee;
        if (dto.getEmployeeId() != null) {
            employee = employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên cần sửa"));
        } else {
            if (employeeRepository.existsByUsername(dto.getUsername())) {
                throw new RuntimeException("Tên đăng nhập '" + dto.getUsername() + "' đã tồn tại");
            }
            employee = new Employee();
            employee.setUsername(dto.getUsername());
            employee.setStartDate(LocalDate.now());
            employee.setBalance(BigDecimal.ZERO);
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            employee.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        employee.setFullName(dto.getFullName());
        employee.setDateOfBirth(dto.getDateOfBirth());
        employee.setGender(dto.getGender());
        employee.setAddress(dto.getAddress());
        employee.setPhone(dto.getPhone());
        employee.setPosition(dto.getPosition());
        employee.setWorkStatus(dto.getWorkStatus() != null ? dto.getWorkStatus() : "ACTIVE");

        if (dto.getBalance() != null && "ROLE_ADMIN".equalsIgnoreCase(dto.getPosition())) {
            employee.setBalance(dto.getBalance());
        }

        if (dto.getWarehouseId() != null) {
            Warehouse w = warehouseRepository.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy kho"));
            employee.setWarehouse(w);
        }

        Employee saved = employeeRepository.save(employee);
        return toDTO(saved);
    }

    @Transactional
    public EmployeeDTO toggleEmployeeStatus(Long employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        String newStatus = "ACTIVE".equalsIgnoreCase(emp.getWorkStatus()) ? "INACTIVE" : "ACTIVE";
        emp.setWorkStatus(newStatus);
        Employee saved = employeeRepository.save(emp);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeDTO getCurrentUserProfile() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new RuntimeException("Chưa đăng nhập hoặc phiên làm việc đã kết thúc");
        }
        Employee emp = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));
        return toDTO(emp);
    }

    @Transactional
    public EmployeeDTO updateCurrentUserProfile(UpdateProfileRequest req) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new RuntimeException("Chưa đăng nhập hoặc phiên làm việc đã kết thúc");
        }
        Employee emp = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));

        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            emp.setFullName(req.getFullName().trim());
        }
        if (req.getDateOfBirth() != null) {
            emp.setDateOfBirth(req.getDateOfBirth());
        }
        if (req.getGender() != null) {
            emp.setGender(req.getGender());
        }
        if (req.getPhone() != null) {
            emp.setPhone(req.getPhone().trim());
        }
        if (req.getAddress() != null) {
            emp.setAddress(req.getAddress().trim());
        }

        // Đổi mật khẩu nếu có yêu cầu
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            if (req.getCurrentPassword() != null && !req.getCurrentPassword().isBlank()) {
                if (!passwordEncoder.matches(req.getCurrentPassword(), emp.getPassword())) {
                    throw new RuntimeException("Mật khẩu hiện tại không chính xác");
                }
            }
            if (req.getNewPassword().length() < 6) {
                throw new RuntimeException("Mật khẩu mới phải có tối thiểu 6 ký tự");
            }
            emp.setPassword(passwordEncoder.encode(req.getNewPassword().trim()));
        }

        Employee saved = employeeRepository.save(emp);
        return toDTO(saved);
    }

    private EmployeeDTO toDTO(Employee e) {
        Integer age = null;
        if (e.getDateOfBirth() != null) {
            age = java.time.Period.between(e.getDateOfBirth(), LocalDate.now()).getYears();
        }
        return EmployeeDTO.builder()
                .employeeId(e.getEmployeeId())
                .username(e.getUsername())
                .fullName(e.getFullName())
                .dateOfBirth(e.getDateOfBirth())
                .age(age)
                .gender(e.getGender())
                .address(e.getAddress())
                .phone(e.getPhone())
                .startDate(e.getStartDate())
                .position(e.getPosition())
                .workStatus(e.getWorkStatus())
                .balance(e.getBalance())
                .warehouseId(e.getWarehouse() != null ? e.getWarehouse().getWarehouseId() : null)
                .warehouseName(e.getWarehouse() != null ? e.getWarehouse().getWarehouseName() : null)
                .build();
    }
}
