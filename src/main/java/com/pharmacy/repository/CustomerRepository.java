package com.pharmacy.repository;

import com.pharmacy.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);

    @Query("SELECT c FROM Customer c WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "c.phone LIKE CONCAT('%', :keyword, '%') OR " +
           "CAST(c.dateOfBirth AS string) LIKE CONCAT('%', :keyword, '%')) " +
           "ORDER BY c.customerId DESC")
    List<Customer> liveSearchCustomers(@Param("keyword") String keyword);
}
