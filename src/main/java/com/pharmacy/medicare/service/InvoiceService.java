package com.pharmacy.medicare.service;

import com.pharmacy.medicare.dto.invoice.*;
import com.pharmacy.medicare.entity.*;
import com.pharmacy.medicare.entity.enums.*;
import com.pharmacy.medicare.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PdfReceiptService pdfReceiptService;

    @Value("${pharmacy.invoice.expiration-minutes:10}")
    private int expirationMinutes;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          ProductRepository productRepository,
                          BatchRepository batchRepository,
                          UserRepository userRepository,
                          NotificationRepository notificationRepository,
                          PdfReceiptService pdfReceiptService) {
        this.invoiceRepository = invoiceRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.pdfReceiptService = pdfReceiptService;
    }

    /**
     * Lập hóa đơn mới và gửi thông báo xác nhận đến Nhân viên bán hàng
     */
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request, User customer) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải chứa ít nhất 1 sản phẩm");
        }

        // Chọn nhân viên bán hàng: nếu khách không chọn, gán NV bán hàng đầu tiên (Nhân viên 1)
        User salesStaff;
        if (request.getSalesStaffId() != null) {
            salesStaff = userRepository.findById(request.getSalesStaffId())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhân viên bán hàng với ID: " + request.getSalesStaffId()));
        } else {
            salesStaff = userRepository.findByRoleName(RoleName.ROLE_SALES).stream()
                    .findFirst()
                    .orElseGet(() -> userRepository.findByRoleName(RoleName.ROLE_ADMIN).stream()
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Hệ thống chưa có người phụ trách bán hàng!")));
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<InvoiceItem> itemsToPersist = new ArrayList<>();

        for (InvoiceItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy sản phẩm ID: " + itemReq.getProductId()));

            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng sản phẩm mua phải lớn hơn 0");
            }

            BigDecimal unitPrice = product.getSellingPrice();
            InvoiceItem invoiceItem = new InvoiceItem(product, itemReq.getQuantity(), unitPrice);
            totalAmount = totalAmount.add(invoiceItem.getSubTotal());
            itemsToPersist.add(invoiceItem);
        }

        Invoice invoice = new Invoice(
                customer,
                salesStaff,
                totalAmount,
                request.getPaymentMethod(),
                expirationMinutes
        );
        invoice.setNote(request.getNote());

        for (InvoiceItem item : itemsToPersist) {
            invoice.addItem(item);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Gửi thông báo đến Nhân viên bán hàng
        String paymentDesc = switch (request.getPaymentMethod()) {
            case CASH -> "Tiền mặt";
            case QR_TRANSFER -> "Chuyển khoản QR Admin";
            case SYSTEM_BALANCE -> "Số dư hệ thống";
        };

        String notifMsg = String.format("Khách hàng %s vừa tạo hóa đơn #%d trị giá %,.0f VND (PT: %s). Vui lòng kiểm tra và xác nhận thanh toán!",
                customer.getFullName(), savedInvoice.getId(), totalAmount, paymentDesc);

        Notification notification = new Notification(
                customer,
                salesStaff,
                NotificationType.PAYMENT_CONFIRM,
                savedInvoice.getId(),
                notifMsg
        );
        notificationRepository.save(notification);

        log.info("-> Đã tạo hóa đơn #{} (Khách: {}, NV: {}, Tổng: {} VND). Hạn thanh toán 10 phút.",
                savedInvoice.getId(), customer.getUsername(), salesStaff.getUsername(), totalAmount);

        return InvoiceResponse.fromEntity(savedInvoice);
    }

    /**
     * Khách hàng hoặc Nhân viên bán hàng chủ động hủy hóa đơn chưa thanh toán
     */
    @Transactional
    public InvoiceResponse cancelInvoice(Long invoiceId, User currentUser) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy hóa đơn với ID: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.UNPAID) {
            throw new IllegalStateException("Chỉ có thể hủy hóa đơn đang ở trạng thái Chưa thanh toán (UNPAID)!");
        }

        // Kiểm tra quyền: Chỉ khách sở hữu hóa đơn, hoặc NV Bán hàng, hoặc Admin mới được hủy
        boolean isOwner = invoice.getCustomer().getId().equals(currentUser.getId());
        boolean isStaffOrAdmin = currentUser.hasRole(RoleName.ROLE_SALES) || currentUser.hasRole(RoleName.ROLE_ADMIN);
        if (!isOwner && !isStaffOrAdmin) {
            throw new AccessDeniedException("Bạn không có quyền hủy hóa đơn này!");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setResultState("fail");
        Invoice saved = invoiceRepository.save(invoice);

        // Cập nhật thông báo liên quan nếu có
        List<Notification> notifs = notificationRepository.findByTypeAndReferenceId(NotificationType.PAYMENT_CONFIRM, invoiceId);
        for (Notification n : notifs) {
            n.setActionStatus(ActionStatus.REJECTED);
            notificationRepository.save(n);
        }

        log.info("-> Hóa đơn #{} đã bị hủy bởi người dùng {}. Kết quả: fail.", saved.getId(), currentUser.getUsername());
        return InvoiceResponse.fromEntity(saved);
    }

    /**
     * Xử lý xác nhận thanh toán từ Nhân viên bán hàng (ROLE_SALES hoặc ROLE_ADMIN)
     */
    @Transactional
    public ConfirmPaymentResponse confirmPayment(Long invoiceId, User staffUser) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy hóa đơn ID: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Hóa đơn này đã được thanh toán trước đó!");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.isExpired()) {
            invoice.setStatus(InvoiceStatus.CANCELLED);
            invoice.setResultState("fail");
            invoiceRepository.save(invoice);
            return new ConfirmPaymentResponse(false, "fail", "Hóa đơn đã bị hủy hoặc đã quá thời hạn 10 phút!", InvoiceResponse.fromEntity(invoice));
        }

        // 1. Kiểm tra tồn kho của từng sản phẩm trong hóa đơn
        for (InvoiceItem item : invoice.getItems()) {
            Product product = item.getProduct();
            if (product.getTotalStock() < item.getQuantity()) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
                invoice.setResultState("fail");
                invoiceRepository.save(invoice);
                return new ConfirmPaymentResponse(
                        false,
                        "fail",
                        "Không thành công: Sản phẩm '" + product.getName() + "' không đủ số lượng tồn kho (Cần: " + item.getQuantity() + ", Còn: " + product.getTotalStock() + ")",
                        InvoiceResponse.fromEntity(invoice)
                );
            }
        }

        // 2. Nếu khách thanh toán bằng Số dư hệ thống (SYSTEM_BALANCE): kiểm tra số dư
        User customer = invoice.getCustomer();
        if (invoice.getPaymentMethod() == PaymentMethod.SYSTEM_BALANCE) {
            if (customer.getBalance().compareTo(invoice.getTotalAmount()) < 0) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
                invoice.setResultState("fail");
                invoiceRepository.save(invoice);
                return new ConfirmPaymentResponse(
                        false,
                        "fail",
                        "Không thành công: Số dư tài khoản của khách hàng không đủ chi trả (Số dư: " + customer.getBalance() + " VND, Cần: " + invoice.getTotalAmount() + " VND)",
                        InvoiceResponse.fromEntity(invoice)
                );
            }
        }

        // 3. ĐIỀU KIỆN THỎA MÃN (Trường hợp TRUE):
        // a. Trừ số lượng tồn kho của sản phẩm và các lô hàng tương ứng (FIFO)
        for (InvoiceItem item : invoice.getItems()) {
            Product product = item.getProduct();
            int remainingToDeduct = item.getQuantity();

            // Cập nhật tổng tồn kho sản phẩm
            product.setTotalStock(product.getTotalStock() - remainingToDeduct);
            productRepository.save(product);

            // Trừ số lượng trong các lô thuốc AVAILABLE theo thứ tự HSD sớm nhất
            List<Batch> activeBatches = batchRepository.findByProductIdAndStatus(product.getId(), BatchStatus.AVAILABLE);
            activeBatches.sort(Comparator.comparing(Batch::getExpDate));

            for (Batch batch : activeBatches) {
                if (remainingToDeduct <= 0) break;
                if (batch.getQuantity() <= remainingToDeduct) {
                    remainingToDeduct -= batch.getQuantity();
                    batch.setQuantity(0);
                } else {
                    batch.setQuantity(batch.getQuantity() - remainingToDeduct);
                    remainingToDeduct = 0;
                }
                batchRepository.save(batch);
            }
        }

        // b. Nếu thanh toán bằng số dư hệ thống: Trừ tiền khách hàng
        if (invoice.getPaymentMethod() == PaymentMethod.SYSTEM_BALANCE) {
            customer.setBalance(customer.getBalance().subtract(invoice.getTotalAmount()));
            userRepository.save(customer);
        }

        // c. Cộng số tiền hóa đơn vào Số dư của Chủ cửa hàng (Admin)
        User admin = userRepository.findByRoleName(RoleName.ROLE_ADMIN).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản Chủ cửa hàng (Admin)!"));
        admin.setBalance(admin.getBalance().add(invoice.getTotalAmount()));
        userRepository.save(admin);

        // d. Chuyển trạng thái hóa đơn thành PAID, kết quả là true
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setResultState("true");
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // e. Cập nhật thông báo
        List<Notification> notifs = notificationRepository.findByTypeAndReferenceId(NotificationType.PAYMENT_CONFIRM, invoiceId);
        for (Notification n : notifs) {
            n.setActionStatus(ActionStatus.ACCEPTED);
            n.setIsRead(true);
            notificationRepository.save(n);
        }

        log.info("-> Xác nhận thanh toán hóa đơn #{} THÀNH CÔNG (resultState=true). Doanh thu +{} VND vào tài khoản Admin.",
                savedInvoice.getId(), savedInvoice.getTotalAmount());

        return new ConfirmPaymentResponse(
                true,
                "true",
                "Thanh toán thành công!",
                InvoiceResponse.fromEntity(savedInvoice)
        );
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long invoiceId, User currentUser) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy hóa đơn ID: " + invoiceId));

        boolean isOwner = invoice.getCustomer().getId().equals(currentUser.getId());
        boolean isStaffOrAdmin = currentUser.hasRole(RoleName.ROLE_SALES) || currentUser.hasRole(RoleName.ROLE_ADMIN);
        if (!isOwner && !isStaffOrAdmin) {
            throw new AccessDeniedException("Bạn không có quyền xem hóa đơn này!");
        }

        return InvoiceResponse.fromEntity(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getMyInvoices(User customer) {
        return invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(InvoiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices(String statusFilter, String keyword) {
        List<Invoice> list;
        if (StringUtils.hasText(statusFilter) && !"ALL".equalsIgnoreCase(statusFilter)) {
            try {
                InvoiceStatus status = InvoiceStatus.valueOf(statusFilter.trim());
                list = invoiceRepository.findByStatusOrderByCreatedAtDesc(status);
            } catch (IllegalArgumentException e) {
                list = invoiceRepository.findAllByOrderByCreatedAtDesc();
            }
        } else {
            list = invoiceRepository.findAllByOrderByCreatedAtDesc();
        }

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            list = list.stream()
                    .filter(inv -> String.valueOf(inv.getId()).contains(kw) ||
                            (inv.getCustomer() != null && inv.getCustomer().getFullName().toLowerCase().contains(kw)) ||
                            (inv.getSalesStaff() != null && inv.getSalesStaff().getFullName().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        return list.stream()
                .map(InvoiceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] getInvoicePdf(Long invoiceId, User currentUser) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy hóa đơn ID: " + invoiceId));

        boolean isOwner = invoice.getCustomer().getId().equals(currentUser.getId());
        boolean isStaffOrAdmin = currentUser.hasRole(RoleName.ROLE_SALES) || currentUser.hasRole(RoleName.ROLE_ADMIN);
        if (!isOwner && !isStaffOrAdmin) {
            throw new AccessDeniedException("Bạn không có quyền tải hóa đơn này!");
        }

        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new IllegalStateException("Chỉ in hóa đơn khi hóa đơn đã được thanh toán thành công (Trạng thái hiện tại: " + invoice.getStatus() + ")!");
        }

        return pdfReceiptService.generateThermalReceipt80mm(invoice);
    }
}
