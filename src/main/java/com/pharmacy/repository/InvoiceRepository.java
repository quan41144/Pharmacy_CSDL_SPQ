package com.pharmacy.repository;

import com.pharmacy.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByOrderByInvoiceIdDesc();

    List<Invoice> findByStatusOrderByInvoiceIdDesc(String status);

    @Query("SELECT i FROM Invoice i WHERE i.createdAt >= :start AND i.createdAt <= :end ORDER BY i.invoiceId DESC")
    List<Invoice> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = 'PAID' AND i.createdAt >= :start AND i.createdAt <= :end")
    BigDecimal sumPaidAmountByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
