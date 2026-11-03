package com.pharmacy.repository;

import com.pharmacy.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByProductProductId(Long productId);

    Optional<Batch> findByBatchCode(String batchCode);

    @Query("SELECT b FROM Batch b JOIN Inventory i ON i.batch.batchId = b.batchId " +
           "WHERE i.warehouse.warehouseId = :warehouseId " +
           "AND b.product.productId = :productId " +
           "AND i.quantity > 0 " +
           "AND b.expDate > :currentDate " +
           "ORDER BY b.expDate ASC")
    List<Batch> findAvailableBatchesForSaleFEFO(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId,
            @Param("currentDate") LocalDate currentDate
    );

    @Query("SELECT b FROM Batch b JOIN Inventory i ON i.batch.batchId = b.batchId " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(b.batchCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.product.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:warehouseId IS NULL OR i.warehouse.warehouseId = :warehouseId) " +
           "AND i.quantity > 0")
    List<Batch> searchBatchesForDisposal(
            @Param("keyword") String keyword,
            @Param("warehouseId") Long warehouseId
    );
}
