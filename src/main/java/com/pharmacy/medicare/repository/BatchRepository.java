package com.pharmacy.medicare.repository;

import com.pharmacy.medicare.entity.Batch;
import com.pharmacy.medicare.entity.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByProductId(Long productId);

    List<Batch> findByStatus(BatchStatus status);

    List<Batch> findByProductIdAndStatus(Long productId, BatchStatus status);

    List<Batch> findByProductIdAndStatusOrderByExpDateAsc(Long productId, BatchStatus status);

    List<Batch> findAllByOrderByCreatedAtDesc();

    List<Batch> findByStatusOrderByCreatedAtDesc(BatchStatus status);

    // Tìm các lô hàng có thể hủy (đã hỏng hoặc hết hạn)
    @Query("SELECT b FROM Batch b WHERE b.status = 'AVAILABLE' AND (b.isDamaged = true OR b.expDate <= :currentDate)")
    List<Batch> findDisposalCandidates(@Param("currentDate") LocalDate currentDate);

    // Live search các lô hàng phục vụ việc lập phiếu hủy
    @Query("SELECT b FROM Batch b WHERE " +
           "(CAST(b.id AS string) LIKE CONCAT('%', :query, '%') OR " +
           " LOWER(b.product.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "b.status = 'AVAILABLE'")
    List<Batch> searchAvailableBatchesForDisposal(@Param("query") String query);
}
