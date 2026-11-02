package com.pharmacy.medicare.service;

import com.pharmacy.medicare.entity.Invoice;
import com.pharmacy.medicare.entity.Notification;
import com.pharmacy.medicare.entity.enums.ActionStatus;
import com.pharmacy.medicare.entity.enums.InvoiceStatus;
import com.pharmacy.medicare.entity.enums.NotificationType;
import com.pharmacy.medicare.repository.InvoiceRepository;
import com.pharmacy.medicare.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class InvoiceScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(InvoiceScheduledTask.class);

    private final InvoiceRepository invoiceRepository;
    private final NotificationRepository notificationRepository;

    public InvoiceScheduledTask(InvoiceRepository invoiceRepository,
                                NotificationRepository notificationRepository) {
        this.invoiceRepository = invoiceRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Tự động quét và xử lý hủy các hóa đơn UNPAID đã quá thời hạn 10 phút.
     * Chạy định kỳ mỗi 30 giây (30000ms).
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void autoCancelExpiredInvoices() {
        LocalDateTime now = LocalDateTime.now();
        List<Invoice> expiredInvoices = invoiceRepository.findByStatusAndExpiresAtBefore(InvoiceStatus.UNPAID, now);

        if (!expiredInvoices.isEmpty()) {
            log.info("=== SCHEDULED JOB: Phát hiện {} hóa đơn quá hạn 10 phút. Tiến hành Rollback & Hủy tự động ===", expiredInvoices.size());
            for (Invoice invoice : expiredInvoices) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
                invoice.setResultState("fail");
                invoiceRepository.save(invoice);

                // Cập nhật thông báo xác nhận sang REJECTED
                List<Notification> notifs = notificationRepository.findByTypeAndReferenceId(NotificationType.PAYMENT_CONFIRM, invoice.getId());
                for (Notification n : notifs) {
                    n.setActionStatus(ActionStatus.REJECTED);
                    notificationRepository.save(n);
                }

                log.info("-> Đã Rollback và hủy hóa đơn quá hạn #{}. Trạng thái kết quả: fail.", invoice.getId());
            }
        }
    }
}
