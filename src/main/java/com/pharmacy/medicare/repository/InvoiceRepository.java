package com.pharmacy.medicare.repository;

import com.pharmacy.medicare.entity.Invoice;
import com.pharmacy.medicare.entity.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Invoice> findBySalesStaffIdOrderByCreatedAtDesc(Long salesStaffId);

    List<Invoice> findAllByOrderByCreatedAtDesc();

    List<Invoice> findByStatusOrderByCreatedAtDesc(InvoiceStatus status);

    // Tìm các hóa đơn UNPAID đã quá hạn 10 phút để tự động Rollback và chuyển CANCELLED
    List<Invoice> findByStatusAndExpiresAtBefore(InvoiceStatus status, LocalDateTime now);

    // Thống kê doanh thu các đơn hàng PAID trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.status = 'PAID' AND i.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenueBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // Lấy danh sách hóa đơn PAID trong khoảng thời gian
    @Query("SELECT i FROM Invoice i WHERE i.status = 'PAID' AND i.createdAt BETWEEN :startDate AND :endDate ORDER BY i.createdAt DESC")
    List<Invoice> findPaidInvoicesBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
}
