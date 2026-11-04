package com.pharmacy.service;

import com.pharmacy.dto.*;
import com.pharmacy.entity.*;
import com.pharmacy.repository.*;
import com.pharmacy.util.MoneyToWordsUtil;
import com.pharmacy.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final WarehouseRepository warehouseRepository;
    private final BatchRepository batchRepository;
    private final JdbcTemplate jdbcTemplate;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Transactional
    public InvoiceResponseDTO createInvoice(CreateInvoiceRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new RuntimeException("Chưa đăng nhập");
        }

        Employee staff = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên bán hàng"));

        if (!"ACTIVE".equalsIgnoreCase(staff.getWorkStatus())) {
            throw new RuntimeException("Nhân viên đã ngừng làm việc, không được lập hóa đơn.");
        }

        if (!"ROLE_SALES".equalsIgnoreCase(staff.getPosition()) && !"ROLE_ADMIN".equalsIgnoreCase(staff.getPosition())) {
            throw new RuntimeException("Nhân viên kho không được bán hàng. Chỉ nhân viên bán hàng hoặc quản trị viên mới được tạo hóa đơn.");
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kho"));

        if (!"ACTIVE".equalsIgnoreCase(warehouse.getStatus())) {
            throw new RuntimeException("Kho đã ngừng hoạt động, không được tạo hóa đơn.");
        }

        Invoice invoice = Invoice.builder()
                .customer(customer)
                .salesStaff(staff)
                .warehouse(warehouse)
                .createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .status("UNPAID")
                .note(request.getNote())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        BigDecimal total = BigDecimal.ZERO;
        List<InvoiceItem> items = new ArrayList<>();

        // Quy ước đặc tả: Một lô xuất hiện tối đa một lần trong một chứng từ; nếu chọn lại cùng lô thì gộp số lượng.
        Map<Long, InvoiceItemRequest> mergedItemsMap = new LinkedHashMap<>();
        for (InvoiceItemRequest itemReq : request.getItems()) {
            if (mergedItemsMap.containsKey(itemReq.getBatchId())) {
                InvoiceItemRequest existing = mergedItemsMap.get(itemReq.getBatchId());
                existing.setQuantity(existing.getQuantity() + itemReq.getQuantity());
            } else {
                mergedItemsMap.put(itemReq.getBatchId(), new InvoiceItemRequest(itemReq.getBatchId(), itemReq.getQuantity(), itemReq.getUnitPrice()));
            }
        }

        for (InvoiceItemRequest itemReq : mergedItemsMap.values()) {
            Batch batch = batchRepository.findById(itemReq.getBatchId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lô hàng ID: " + itemReq.getBatchId()));

            BigDecimal price = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : batch.getProduct().getSellingPrice();
            BigDecimal subTotal = price.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            InvoiceItemId itemId = InvoiceItemId.builder()
                    .invoiceId(savedInvoice.getInvoiceId())
                    .batchId(batch.getBatchId())
                    .build();

            InvoiceItem item = InvoiceItem.builder()
                    .id(itemId)
                    .invoice(savedInvoice)
                    .batch(batch)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(price)
                    .subTotal(subTotal)
                    .build();

            items.add(invoiceItemRepository.save(item));
            total = total.add(subTotal);
        }

        savedInvoice.setTotalAmount(total);
        savedInvoice.setItems(items);
        invoiceRepository.save(savedInvoice);

        return toDTO(savedInvoice);
    }

    @Transactional
    public InvoiceResponseDTO confirmPayment(Long invoiceId) {
        // Gọi Stored Procedure kiểm tra hạn dùng, kiểm tra tồn kho, trừ kho và cộng balance Admin
        try {
            jdbcTemplate.execute("CALL sp_confirm_invoice_payment(" + invoiceId + ")");
            entityManager.clear();
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("ERROR:")) {
                int start = msg.indexOf("ERROR:") + 6;
                int end = msg.indexOf("\n", start);
                if (end > start) {
                    msg = msg.substring(start, end).trim();
                } else {
                    msg = msg.substring(start).trim();
                }
            }
            throw new RuntimeException("Xác nhận thanh toán thất bại: " + msg, e);
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        return toDTO(invoice);
    }

    @Transactional
    public InvoiceResponseDTO cancelInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        if (!"UNPAID".equalsIgnoreCase(invoice.getStatus())) {
            throw new RuntimeException("Chỉ được hủy hóa đơn khi đang ở trạng thái chưa thanh toán (UNPAID)");
        }

        invoice.setStatus("CANCELLED");
        Invoice saved = invoiceRepository.save(invoice);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceResponseDTO getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        return toDTO(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponseDTO> getAllInvoices() {
        return invoiceRepository.findAllByOrderByInvoiceIdDesc().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponseDTO> searchInvoices(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllInvoices();
        }
        String cleanKw = keyword.trim();
        Long idFromCode = null;
        if (cleanKw.toUpperCase().startsWith("PT")) {
            try {
                idFromCode = Long.parseLong(cleanKw.substring(2));
            } catch (NumberFormatException ignored) {}
        } else {
            try {
                idFromCode = Long.parseLong(cleanKw);
            } catch (NumberFormatException ignored) {}
        }
        return invoiceRepository.searchInvoicesByCustomerOrCode(cleanKw, idFromCode).stream()
                .map(this::toDTO)
                .toList();
    }

    public InvoiceResponseDTO toDTO(Invoice inv) {
        String code = String.format("PT%05d", inv.getInvoiceId());
        String words = MoneyToWordsUtil.convertToVietnameseWords(inv.getTotalAmount());

        List<InvoiceItemResponseDTO> itemDTOs = new ArrayList<>();
        if (inv.getItems() != null) {
            int seq = 1;
            for (InvoiceItem it : inv.getItems()) {
                itemDTOs.add(InvoiceItemResponseDTO.builder()
                        .batchId(it.getBatch().getBatchId())
                        .batchCode(it.getBatch().getBatchCode())
                        .productId(it.getBatch().getProduct().getProductId())
                        .productName(it.getBatch().getProduct().getProductName())
                        .unit(it.getBatch().getProduct().getUnit())
                        .quantity(it.getQuantity())
                        .unitPrice(it.getUnitPrice())
                        .subTotal(it.getSubTotal())
                        .build());
            }
        }

        return InvoiceResponseDTO.builder()
                .invoiceId(inv.getInvoiceId())
                .invoiceCode(code)
                .customerId(inv.getCustomer().getCustomerId())
                .customerName(inv.getCustomer().getFullName())
                .customerPhone(inv.getCustomer().getPhone())
                .salesStaffId(inv.getSalesStaff().getEmployeeId())
                .salesStaffName(inv.getSalesStaff().getFullName())
                .warehouseId(inv.getWarehouse().getWarehouseId())
                .warehouseName(inv.getWarehouse().getWarehouseName())
                .createdAt(inv.getCreatedAt())
                .totalAmount(inv.getTotalAmount())
                .totalAmountInWords(words)
                .status(inv.getStatus())
                .note(inv.getNote())
                .items(itemDTOs)
                .build();
    }
}
