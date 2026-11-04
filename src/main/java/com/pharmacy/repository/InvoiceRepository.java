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

    @Query("SELECT i FROM Invoice i LEFT JOIN i.customer c " +
           "WHERE (:keyword IS NULL OR :keyword = '' " +
           "   OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR c.phone LIKE CONCAT('%', :keyword, '%') " +
           "   OR (:idFromCode IS NOT NULL AND i.invoiceId = :idFromCode)) " +
           "ORDER BY i.invoiceId DESC")
    List<Invoice> searchInvoicesByCustomerOrCode(@Param("keyword") String keyword, @Param("idFromCode") Long idFromCode);

    @Query("SELECT i FROM Invoice i WHERE i.createdAt >= :start AND i.createdAt <= :end ORDER BY i.invoiceId DESC")
    List<Invoice> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = 'PAID' AND i.createdAt >= :start AND i.createdAt <= :end")
    BigDecimal sumPaidAmountByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
