package com.pharmacy.repository;

import com.pharmacy.entity.ImportSlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImportSlipRepository extends JpaRepository<ImportSlip, Long> {
    List<ImportSlip> findByStatusOrderByImportSlipIdDesc(String status);

    List<ImportSlip> findAllByOrderByImportSlipIdDesc();

    @Query("SELECT i FROM ImportSlip i WHERE i.importDate >= :start AND i.importDate <= :end ORDER BY i.importSlipId DESC")
    List<ImportSlip> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM ImportSlip i WHERE i.status = 'IMPORTED' AND i.importDate >= :start AND i.importDate <= :end")
    BigDecimal sumImportAmountByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
