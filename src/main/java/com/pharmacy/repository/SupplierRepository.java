package com.pharmacy.repository;

import com.pharmacy.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    @Query("SELECT s FROM Supplier s WHERE " +
           ":keyword IS NULL OR :keyword = '' OR " +
           "LOWER(s.supplierName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "s.phone LIKE CONCAT('%', :keyword, '%') OR " +
           "s.taxCode LIKE CONCAT('%', :keyword, '%') OR " +
           "LOWER(s.representativeName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.address) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY s.supplierId ASC")
    List<Supplier> liveSearchSuppliers(@Param("keyword") String keyword);
}
