package com.pharmacy.medicare.repository;

import com.pharmacy.medicare.entity.WarehouseSlipItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseSlipItemRepository extends JpaRepository<WarehouseSlipItem, Long> {
    List<WarehouseSlipItem> findByWarehouseSlipId(Long warehouseSlipId);
    List<WarehouseSlipItem> findByBatchId(Long batchId);
}
