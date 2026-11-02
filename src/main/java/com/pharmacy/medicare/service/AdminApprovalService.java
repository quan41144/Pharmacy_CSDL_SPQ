package com.pharmacy.medicare.service;

import com.pharmacy.medicare.dto.invoice.InvoiceResponse;
import com.pharmacy.medicare.dto.statistics.FinancialStatisticsResponse;
import com.pharmacy.medicare.dto.warehouse.BatchResponse;
import com.pharmacy.medicare.dto.warehouse.WarehouseSlipResponse;
import com.pharmacy.medicare.entity.*;
import com.pharmacy.medicare.entity.enums.*;
import com.pharmacy.medicare.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class AdminApprovalService {

    private static final Logger log = LoggerFactory.getLogger(AdminApprovalService.class);

    private final WarehouseSlipRepository warehouseSlipRepository;
    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final InvoiceRepository invoiceRepository;

    public AdminApprovalService(WarehouseSlipRepository warehouseSlipRepository,
                                BatchRepository batchRepository,
                                ProductRepository productRepository,
                                UserRepository userRepository,
                                NotificationRepository notificationRepository,
                                InvoiceRepository invoiceRepository) {
        this.warehouseSlipRepository = warehouseSlipRepository;
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.invoiceRepository = invoiceRepository;
    }

    private boolean isPositiveDecision(String decision) {
        if (decision == null) return false;
        String d = decision.trim().toUpperCase();
        return "ACCEPT".equals(d)
                || "AGREE".equals(d)
                || "APPROVED".equals(d)
                || "APPROVE".equals(d)
                || "YES".equals(d)
                || "TRUE".equals(d);
    }

    /**
     * Admin xử lý phê duyệt trực tiếp trên Thông báo (Phiếu nhập / Phiếu hủy)
     */
    @Transactional
    public String handleNotificationAction(Long notificationId, String decision, User admin) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thông báo ID: " + notificationId));

        if (notification.getActionStatus() != ActionStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý trước đó!");
        }

        boolean isAccept = isPositiveDecision(decision);
        String resultMsg;

        if (notification.getType() == NotificationType.IMPORT_CONFIRM) {
            resultMsg = processImportConfirmation(notification.getReferenceId(), isAccept, admin);
        } else if (notification.getType() == NotificationType.DISPOSAL_CONFIRM) {
            resultMsg = processDisposalConfirmation(notification.getReferenceId(), isAccept, admin);
        } else {
            resultMsg = isAccept ? "Đã chấp nhận thông báo" : "Đã từ chối thông báo";
        }

        boolean actionSucceeded = !resultMsg.startsWith("Không thành công");
        if (actionSucceeded) {
            notification.setActionStatus(isAccept ? ActionStatus.ACCEPTED : ActionStatus.REJECTED);
        } else {
            notification.setActionStatus(ActionStatus.REJECTED);
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);

        return resultMsg;
    }

    /**
     * Admin xử lý trực tiếp trên phiếu nhập/hủy
     */
    @Transactional
    public String handleSlipDirectAction(Long slipId, String decision, User admin) {
        WarehouseSlip slip = warehouseSlipRepository.findById(slipId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu ID: " + slipId));

        boolean isAccept = isPositiveDecision(decision);
        String resultMsg;
        if (slip.getType() == SlipType.IMPORT) {
            resultMsg = processImportConfirmation(slipId, isAccept, admin);
        } else if (slip.getType() == SlipType.DISPOSAL) {
            resultMsg = processDisposalConfirmation(slipId, isAccept, admin);
        } else {
            throw new IllegalArgumentException("Loại phiếu không hỗ trợ phê duyệt: " + slip.getType());
        }

        // Cập nhật thông báo PENDING tương ứng nếu có
        List<Notification> notifs = notificationRepository.findAll().stream()
                .filter(n -> slipId.equals(n.getReferenceId()) && n.getActionStatus() == ActionStatus.PENDING)
                .collect(Collectors.toList());
        for (Notification n : notifs) {
            if (resultMsg.startsWith("Không thành công")) {
                n.setActionStatus(ActionStatus.REJECTED);
            } else {
                n.setActionStatus(isAccept ? ActionStatus.ACCEPTED : ActionStatus.REJECTED);
            }
            n.setIsRead(true);
            notificationRepository.save(n);
        }

        return resultMsg;
    }

    public String processImportConfirmation(Long slipId, boolean isAccept, User admin) {
        WarehouseSlip slip = warehouseSlipRepository.findById(slipId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu nhập ID: " + slipId));

        if (slip.getStatus() != SlipStatus.PENDING) {
            throw new IllegalStateException("Phiếu nhập #" + slipId + " đã được xử lý trước đó (Trạng thái: " + slip.getStatus() + ")!");
        }

        if (!isAccept) {
            slip.setStatus(SlipStatus.REJECTED);
            slip.setResultState("fail");
            if (slip.getItems() != null) {
                for (WarehouseSlipItem item : slip.getItems()) {
                    if (item.getBatch() != null) {
                        Batch b = batchRepository.findById(item.getBatch().getId()).orElse(item.getBatch());
                        b.setStatus(BatchStatus.REJECTED);
                        batchRepository.save(b);
                    }
                }
            }
            warehouseSlipRepository.save(slip);
            log.info("-> Admin từ chối phiếu nhập #{}. Trạng thái: fail.", slip.getId());
            return "Đã từ chối duyệt phiếu nhập #" + slip.getId();
        }

        // Admin bấm Đồng ý: Kiểm tra lại số dư
        admin = userRepository.findById(admin.getId()).orElseThrow();
        if (admin.getBalance().compareTo(slip.getTotalAmount()) < 0) {
            slip.setStatus(SlipStatus.REJECTED);
            slip.setResultState("fail");
            if (slip.getItems() != null) {
                for (WarehouseSlipItem item : slip.getItems()) {
                    if (item.getBatch() != null) {
                        Batch b = batchRepository.findById(item.getBatch().getId()).orElse(item.getBatch());
                        b.setStatus(BatchStatus.REJECTED);
                        batchRepository.save(b);
                    }
                }
            }
            warehouseSlipRepository.save(slip);
            log.warn("-> Duyệt phiếu nhập #{} thất bại: Số dư Admin không còn đủ chi trả.", slip.getId());
            return "Không thành công: Số dư của chủ không đủ chi trả!";
        }

        // Trừ tiền số dư của Admin
        admin.setBalance(admin.getBalance().subtract(slip.getTotalAmount()));
        userRepository.save(admin);

        // Chuyển lô thuốc sang trạng thái "Chờ duyệt để bán" (WAITING_FOR_PRICE)
        if (slip.getItems() != null) {
            for (WarehouseSlipItem item : slip.getItems()) {
                if (item.getBatch() != null) {
                    Batch batch = batchRepository.findById(item.getBatch().getId()).orElse(item.getBatch());
                    batch.setStatus(BatchStatus.WAITING_FOR_PRICE);
                    batchRepository.save(batch);
                }
            }
        }

        slip.setStatus(SlipStatus.APPROVED);
        slip.setResultState("true");
        warehouseSlipRepository.save(slip);

        log.info("-> Admin duyệt phiếu nhập #{} THÀNH CÔNG. Đã trừ {} VND từ tài khoản Admin. Lô thuốc chuyển sang WAITING_FOR_PRICE.",
                slip.getId(), slip.getTotalAmount());
        return "Thành công: Đã duyệt phiếu nhập!";
    }

    public String processDisposalConfirmation(Long slipId, boolean isAccept, User admin) {
        WarehouseSlip slip = warehouseSlipRepository.findById(slipId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu hủy ID: " + slipId));

        if (slip.getStatus() != SlipStatus.PENDING) {
            throw new IllegalStateException("Phiếu hủy #" + slipId + " đã được xử lý trước đó (Trạng thái: " + slip.getStatus() + ")!");
        }

        if (!isAccept) {
            slip.setStatus(SlipStatus.REJECTED);
            slip.setResultState("fail");
            warehouseSlipRepository.save(slip);
            log.info("-> Admin từ chối phiếu hủy #{}. Trạng thái: fail.", slip.getId());
            return "Đã từ chối duyệt phiếu hủy #" + slip.getId();
        }

        if (slip.getItems() == null || slip.getItems().isEmpty()) {
            slip.setStatus(SlipStatus.REJECTED);
            slip.setResultState("fail");
            warehouseSlipRepository.save(slip);
            return "Không thành công: Phiếu hủy không gắn với lô hàng hợp lệ";
        }

        // Kiểm tra điều kiện số lượng trên từng item
        for (WarehouseSlipItem item : slip.getItems()) {
            if (item.getBatch() == null) {
                slip.setStatus(SlipStatus.REJECTED);
                slip.setResultState("fail");
                warehouseSlipRepository.save(slip);
                return "Không thành công: Phiếu hủy chứa mặt hàng không gắn với lô hợp lệ";
            }
            Batch b = batchRepository.findById(item.getBatch().getId()).orElse(item.getBatch());
            if (item.getQuantity() > b.getQuantity()) {
                slip.setStatus(SlipStatus.REJECTED);
                slip.setResultState("fail");
                warehouseSlipRepository.save(slip);
                return "Không thành công: Số lượng hủy vượt quá số lượng tồn kho của lô!";
            }
        }

        // Thỏa mãn điều kiện: Thực hiện trừ tồn kho sản phẩm và trừ tồn kho lô
        for (WarehouseSlipItem item : slip.getItems()) {
            Batch batch = batchRepository.findById(item.getBatch().getId()).orElse(item.getBatch());
            Product product = productRepository.findById(item.getProduct().getId()).orElse(item.getProduct());

            int newTotalStock = Math.max(0, product.getTotalStock() - item.getQuantity());
            product.setTotalStock(newTotalStock);
            productRepository.save(product);

            int remainingBatchQty = batch.getQuantity() - item.getQuantity();
            if (remainingBatchQty <= 0) {
                batch.setQuantity(0);
                batch.setStatus(BatchStatus.CANCELLED); // Xóa mềm bằng trạng thái
            } else {
                batch.setQuantity(remainingBatchQty);
            }
            batchRepository.save(batch);
        }

        slip.setStatus(SlipStatus.APPROVED);
        slip.setResultState("true");
        warehouseSlipRepository.save(slip);

        log.info("-> Admin xác nhận duyệt phiếu hủy #{} THÀNH CÔNG. Trừ {} sản phẩm khỏi kho.", slip.getId(), slip.getQuantity());
        return "Xác nhận hủy thành công!";
    }

    /**
     * Danh sách Lô hàng chờ duyệt giá bán (WAITING_FOR_PRICE)
     */
    @Transactional(readOnly = true)
    public List<BatchResponse> getBatchesWaitingForPrice() {
        return batchRepository.findByStatusOrderByCreatedAtDesc(BatchStatus.WAITING_FOR_PRICE).stream()
                .map(BatchResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Admin Duyệt bán lô hàng & Cập nhật giá bán mới
     */
    @Transactional
    public BatchResponse approveBatchPrice(Long batchId, BigDecimal newSellingPrice, User admin) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy lô hàng với ID: " + batchId));

        if (batch.getStatus() != BatchStatus.WAITING_FOR_PRICE) {
            throw new IllegalStateException("Lô hàng không ở trạng thái Chờ duyệt giá bán (WAITING_FOR_PRICE)!");
        }

        if (newSellingPrice == null || newSellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán mới phải lớn hơn 0!");
        }

        Product product = batch.getProduct();

        // 1. Cập nhật giá bán mới cho sản phẩm trên toàn hệ thống (đồng bộ tất cả tồn kho của sản phẩm)
        product.setSellingPrice(newSellingPrice);

        // 2. Cộng số lượng của lô vào tổng tồn kho mở bán
        product.setTotalStock(product.getTotalStock() + batch.getQuantity());
        productRepository.save(product);

        // 3. Chuyển trạng thái lô thành AVAILABLE
        batch.setStatus(BatchStatus.AVAILABLE);
        Batch savedBatch = batchRepository.save(batch);

        log.info("-> Admin duyệt giá bán mới cho Lô #{} (Sản phẩm: {}, Giá mới: {} VND, Tồn kho mới: {}).",
                savedBatch.getId(), product.getName(), newSellingPrice, product.getTotalStock());

        return BatchResponse.fromEntity(savedBatch);
    }

    /**
     * Thống kê Doanh thu & Dòng tiền theo Ngày / Tháng / Năm
     */
    @Transactional(readOnly = true)
    public FinancialStatisticsResponse getFinancialStatistics(String period, String dateStr) {
        String safePeriod = (period != null) ? period.trim().toLowerCase() : "month";
        LocalDate baseDate = (dateStr != null && !dateStr.isBlank()) ? LocalDate.parse(dateStr.trim()) : LocalDate.now();

        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        switch (safePeriod) {
            case "day" -> {
                startDateTime = baseDate.atStartOfDay();
                endDateTime = baseDate.atTime(LocalTime.MAX);
            }
            case "year" -> {
                LocalDate startOfYear = LocalDate.of(baseDate.getYear(), 1, 1);
                LocalDate endOfYear = LocalDate.of(baseDate.getYear(), 12, 31);
                startDateTime = startOfYear.atStartOfDay();
                endDateTime = endOfYear.atTime(LocalTime.MAX);
            }
            default -> { // "month"
                safePeriod = "month";
                YearMonth ym = YearMonth.of(baseDate.getYear(), baseDate.getMonth());
                startDateTime = ym.atDay(1).atStartOfDay();
                endDateTime = ym.atEndOfMonth().atTime(LocalTime.MAX);
            }
        }

        BigDecimal totalRevenue = invoiceRepository.calculateRevenueBetween(startDateTime, endDateTime);
        BigDecimal totalImport = warehouseSlipRepository.calculateImportExpenseBetween(startDateTime, endDateTime);
        BigDecimal netProfit = totalRevenue.subtract(totalImport);

        User admin = userRepository.findByRoleName(RoleName.ROLE_ADMIN).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản Quản trị viên/Chủ cửa hàng!"));

        List<Invoice> paidInvoices = invoiceRepository.findPaidInvoicesBetween(startDateTime, endDateTime);
        List<WarehouseSlip> importSlips = warehouseSlipRepository.findApprovedImportSlipsBetween(startDateTime, endDateTime);

        FinancialStatisticsResponse response = new FinancialStatisticsResponse();
        response.setPeriod(safePeriod);
        response.setStartDate(startDateTime);
        response.setEndDate(endDateTime);
        response.setTotalRevenue(totalRevenue);
        response.setTotalImportExpense(totalImport);
        response.setNetProfit(netProfit);
        response.setAdminCurrentBalance(admin.getBalance());
        response.setPaidInvoicesCount(paidInvoices.size());
        response.setImportSlipsCount(importSlips.size());
        response.setRecentPaidInvoices(paidInvoices.stream().limit(15).map(InvoiceResponse::fromEntity).collect(Collectors.toList()));
        response.setRecentImportSlips(importSlips.stream().limit(15).map(WarehouseSlipResponse::fromEntity).collect(Collectors.toList()));

        return response;
    }
}
