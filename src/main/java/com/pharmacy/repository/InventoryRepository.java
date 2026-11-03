package com.pharmacy.repository;

import com.pharmacy.entity.Inventory;
import com.pharmacy.entity.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, InventoryId> {
    Optional<Inventory> findByWarehouseWarehouseIdAndBatchBatchId(Long warehouseId, Long batchId);

    List<Inventory> findByWarehouseWarehouseId(Long warehouseId);

    List<Inventory> findByBatchBatchId(Long batchId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i " +
           "WHERE i.warehouse.warehouseId = :warehouseId AND i.batch.product.productId = :productId " +
           "AND i.batch.expDate > CURRENT_DATE")
    Integer getTotalStockByWarehouseAndProduct(@Param("warehouseId") Long warehouseId, @Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i " +
           "WHERE i.batch.product.productId = :productId AND i.batch.expDate > CURRENT_DATE")
    Integer getTotalStockByProduct(@Param("productId") Long productId);

    @Query("SELECT i FROM Inventory i " +
           "JOIN FETCH i.warehouse w " +
           "JOIN FETCH i.batch b " +
           "JOIN FETCH b.product p " +
           "JOIN FETCH p.category c " +
           "WHERE (:warehouseId IS NULL OR w.warehouseId = :warehouseId) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.batchCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.productName ASC, b.expDate ASC")
    List<Inventory> findAllInventoryDetails(@Param("warehouseId") Long warehouseId, @Param("keyword") String keyword);
}
