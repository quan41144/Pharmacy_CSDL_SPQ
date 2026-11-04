package com.pharmacy.repository;

import com.pharmacy.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUsername(String username);

    Optional<Employee> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    List<Employee> findByPositionAndWorkStatus(String position, String workStatus);

    List<Employee> findAllByOrderByEmployeeIdAsc();
}
