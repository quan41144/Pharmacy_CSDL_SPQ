package com.pharmacy.service;

import com.pharmacy.dto.CreateImportSlipRequest;
import com.pharmacy.dto.ImportSlipItemResponseDTO;
import com.pharmacy.dto.ImportSlipResponseDTO;
import com.pharmacy.entity.*;
import com.pharmacy.repository.*;
import com.pharmacy.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportSlipService {

    private final ImportSlipRepository importSlipRepository;
    private final ImportSlipItemRepository importSlipItemRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final EmployeeService employeeService;
    private final JdbcTemplate jdbcTemplate;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Transactional
    public ImportSlipResponseDTO createImportSlip(CreateImportSlipRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new RuntimeException("Chưa đăng nhập");
        }

        Employee staff = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên kho"));

        if (!"ACTIVE".equalsIgnoreCase(staff.getWorkStatus())) {
            throw new RuntimeException("Nhân viên đã ngừng làm việc, không được lập phiếu nhập.");
        }

        if (!"ROLE_WAREHOUSE".equalsIgnoreCase(staff.getPosition()) && !"ROLE_ADMIN".equalsIgnoreCase(staff.getPosition())) {
            throw new RuntimeException("Nhân viên bán hàng không được nhập hàng. Chỉ nhân viên kho hoặc quản trị viên mới được lập phiếu nhập.");
        }

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà cung cấp"));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kho"));

        if (!"ACTIVE".equalsIgnoreCase(warehouse.getStatus())) {
            throw new RuntimeException("Kho đã ngừng hoạt động, không được nhập hàng.");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc cần nhập"));

        if (request.getExpDate().isBefore(request.getMfgDate()) || request.getExpDate().isEqual(request.getMfgDate())) {
            throw new RuntimeException("Hạn sử dụng phải lớn hơn ngày sản xuất");
        }

        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : BigDecimal.valueOf(10000.00);
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        // Tạo hoặc lấy lô thuốc
        Batch batch = batchRepository.findByBatchCode(request.getBatchCode()).orElse(null);
        if (batch == null) {
            batch = Batch.builder()
                    .product(product)
                    .batchCode(request.getBatchCode())
                    .mfgDate(request.getMfgDate())
                    .expDate(request.getExpDate())
                    .build();
            batch = batchRepository.save(batch);
        }

        ImportSlip slip = ImportSlip.builder()
                .supplier(supplier)
                .warehouseStaff(staff)
                .warehouse(warehouse)
                .importDate(LocalDateTime.now())
                .totalAmount(totalAmount)
                .status("PENDING")
                .build();

        ImportSlip savedSlip = importSlipRepository.saveAndFlush(slip);

        ImportSlipItemId itemId = ImportSlipItemId.builder()
                .importSlipId(savedSlip.getImportSlipId())
                .batchId(batch.getBatchId())
                .build();

        ImportSlipItem item = ImportSlipItem.builder()
                .id(itemId)
                .importSlip(savedSlip)
                .batch(batch)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .subTotal(totalAmount)
                .build();

        importSlipItemRepository.saveAndFlush(item);

        return toDTO(savedSlip);
    }

    @Transactional
    public ImportSlipResponseDTO confirmImportSlip(Long importSlipId) {
        try {
            jdbcTemplate.execute("CALL sp_confirm_import_slip(" + importSlipId + ")");
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
            throw new RuntimeException("Phê duyệt phiếu nhập thất bại: " + msg, e);
        }

        ImportSlip slip = importSlipRepository.findById(importSlipId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập"));
        return toDTO(slip);
    }

    @Transactional
    public ImportSlipResponseDTO cancelImportSlip(Long importSlipId) {
        ImportSlip slip = importSlipRepository.findById(importSlipId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập"));

        if (!"PENDING".equalsIgnoreCase(slip.getStatus())) {
            throw new RuntimeException("Chỉ được hủy phiếu nhập khi đang ở trạng thái PENDING");
        }

        slip.setStatus("CANCELLED");
        ImportSlip saved = importSlipRepository.save(slip);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ImportSlipResponseDTO> getAllImportSlips() {
        return importSlipRepository.findAllByOrderByImportSlipIdDesc().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ImportSlipResponseDTO> getPendingImportSlips() {
        return importSlipRepository.findByStatusOrderByImportSlipIdDesc("PENDING").stream().map(this::toDTO).toList();
    }

    public ImportSlipResponseDTO toDTO(ImportSlip slip) {
        List<ImportSlipItemResponseDTO> itemDTOs = new ArrayList<>();
        List<ImportSlipItem> items = (slip.getItems() != null && !slip.getItems().isEmpty())
                ? slip.getItems()
                : importSlipItemRepository.findByImportSlipImportSlipId(slip.getImportSlipId());

        if (items != null) {
            for (ImportSlipItem it : items) {
                itemDTOs.add(ImportSlipItemResponseDTO.builder()
                        .batchId(it.getBatch().getBatchId())
                        .batchCode(it.getBatch().getBatchCode())
                        .productId(it.getBatch().getProduct().getProductId())
                        .productName(it.getBatch().getProduct().getProductName())
                        .unit(it.getBatch().getProduct().getUnit())
                        .mfgDate(it.getBatch().getMfgDate())
                        .expDate(it.getBatch().getExpDate())
                        .quantity(it.getQuantity())
                        .unitPrice(it.getUnitPrice())
                        .subTotal(it.getSubTotal())
                        .build());
            }
        }

        return ImportSlipResponseDTO.builder()
                .importSlipId(slip.getImportSlipId())
                .supplierId(slip.getSupplier().getSupplierId())
                .supplierName(slip.getSupplier().getSupplierName())
                .warehouseStaffId(slip.getWarehouseStaff().getEmployeeId())
                .warehouseStaffName(slip.getWarehouseStaff().getFullName())
                .warehouseId(slip.getWarehouse().getWarehouseId())
                .warehouseName(slip.getWarehouse().getWarehouseName())
                .importDate(slip.getImportDate())
                .totalAmount(slip.getTotalAmount())
                .status(slip.getStatus())
                .items(itemDTOs)
                .build();
    }
}
