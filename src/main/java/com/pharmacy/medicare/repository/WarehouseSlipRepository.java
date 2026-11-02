package com.pharmacy.medicare.repository;

import com.pharmacy.medicare.entity.WarehouseSlip;
import com.pharmacy.medicare.entity.enums.SlipStatus;
import com.pharmacy.medicare.entity.enums.SlipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseSlipRepository extends JpaRepository<WarehouseSlip, Long> {

    List<WarehouseSlip> findByTypeOrderByCreatedAtDesc(SlipType type);

    List<WarehouseSlip> findByStatusOrderByCreatedAtDesc(SlipStatus status);

    List<WarehouseSlip> findAllByOrderByCreatedAtDesc();

    List<WarehouseSlip> findByWarehouseStaffIdOrderByCreatedAtDesc(Long staffId);

    // Tính tổng chi phí nhập hàng thực tế (APPROVED, resultState = true) trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM WarehouseSlip s " +
           "WHERE s.type = 'IMPORT' AND s.status = 'APPROVED' AND s.resultState = 'true' " +
           "AND s.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateImportExpenseBetween(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // Lấy các phiếu nhập đã duyệt trong khoảng thời gian
    @Query("SELECT s FROM WarehouseSlip s WHERE s.type = 'IMPORT' AND s.status = 'APPROVED' AND s.resultState = 'true' " +
           "AND s.createdAt BETWEEN :startDate AND :endDate ORDER BY s.createdAt DESC")
    List<WarehouseSlip> findApprovedImportSlipsBetween(@Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);
}
