package com.pharmacy.medicare.repository;

import com.pharmacy.medicare.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByWarehouseName(String warehouseName);
    boolean existsByWarehouseName(String warehouseName);
}
