package com.pharmacy.medicare.repository;

import com.pharmacy.medicare.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT s FROM Supplier s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(s.representativeName) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :kw, '%')) ORDER BY s.id DESC")
    List<Supplier> searchSuppliers(@Param("kw") String keyword);
}
