package com.pharmacy.medicare.service;

import com.pharmacy.medicare.dto.warehouse.*;
import com.pharmacy.medicare.entity.*;
import com.pharmacy.medicare.entity.enums.*;
import com.pharmacy.medicare.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class WarehouseService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseService.class);

    private final WarehouseSlipRepository warehouseSlipRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;

    public WarehouseService(WarehouseSlipRepository warehouseSlipRepository,
                            ProductRepository productRepository,
                            BatchRepository batchRepository,
                            UserRepository userRepository,
                            NotificationRepository notificationRepository,
                            WarehouseRepository warehouseRepository,
                            SupplierRepository supplierRepository) {
        this.warehouseSlipRepository = warehouseSlipRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.warehouseRepository = warehouseRepository;
        this.supplierRepository = supplierRepository;
    }

    /**
     * Nghiệp vụ 1: Lập Phiếu Nhập (IMPORT) theo từng Lô thuốc
     */
    @Transactional
    public WarehouseSlipResponse createImportSlip(WarehouseSlipRequest request, User warehouseStaff) {
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn sản phẩm cần nhập!");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0!");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy sản phẩm ID: " + request.getProductId()));

        BigDecimal importPrice = product.getImportPrice() != null ? product.getImportPrice() : new BigDecimal("10000.00");
        BigDecimal totalAmount = importPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        // Lấy tài khoản Admin (Chủ cửa hàng)
        User admin = userRepository.findByRoleName(RoleName.ROLE_ADMIN).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản Quản trị viên/Chủ cửa hàng!"));

        Warehouse warehouse = null;
        if (request.getWarehouseId() != null) {
            warehouse = warehouseRepository.findById(request.getWarehouseId()).orElse(null);
        }
        if (warehouse == null) {
            warehouse = warehouseRepository.findAll().stream().findFirst().orElse(null);
        }

        Supplier supplier = null;
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findById(request.getSupplierId()).orElse(null);
        }
        if (supplier == null) {
            supplier = supplierRepository.findAll().stream().findFirst().orElse(null);
        }

        // BƯỚC 1: Kiểm tra ngay số dư hiện tại của Admin
        if (admin.getBalance().compareTo(totalAmount) < 0) {
            // Không đủ số dư: Đánh dấu phiếu là fail, status là REJECTED
            WarehouseSlip failedSlip = new WarehouseSlip(
                    SlipType.IMPORT,
                    warehouseStaff,
                    warehouse,
                    supplier,
                    totalAmount
            );
            failedSlip.setStatus(SlipStatus.REJECTED);
            failedSlip.setResultState("fail");
            WarehouseSlip savedFailed = warehouseSlipRepository.save(failedSlip);
            log.warn("-> Lập phiếu nhập THẤT BẠI: Số dư Admin không đủ chi trả (Cần: {} VND, Số dư: {} VND).",
                    totalAmount, admin.getBalance());
            return WarehouseSlipResponse.fromEntity(savedFailed);
        }

        // BƯỚC 2: Số dư Admin đủ -> Lưu phiếu ở trạng thái PENDING, resultState = pending
        // Đồng thời tạm tạo lô thuốc ở trạng thái PENDING_APPROVAL
        LocalDate mfg = request.getMfgDate() != null ? request.getMfgDate() : LocalDate.now();
        LocalDate exp = request.getExpDate() != null ? request.getExpDate() : LocalDate.now().plusYears(2);

        Batch pendingBatch = new Batch(
                product,
                mfg,
                exp,
                request.getQuantity(),
                importPrice,
                BatchStatus.PENDING_APPROVAL,
                false
        );
        pendingBatch.setWarehouse(warehouse);
        Batch savedBatch = batchRepository.save(pendingBatch);

        WarehouseSlip slip = new WarehouseSlip(
                SlipType.IMPORT,
                warehouseStaff,
                warehouse,
                supplier,
                totalAmount
        );
        WarehouseSlipItem item = new WarehouseSlipItem(
                slip,
                product,
                savedBatch,
                request.getQuantity(),
                importPrice,
                null
        );
        slip.addItem(item);
        WarehouseSlip savedSlip = warehouseSlipRepository.save(slip);

        // Gửi thông báo đến Admin kèm 2 nút Đồng ý / Từ chối
        String message = String.format("NV Kho %s yêu cầu duyệt Phiếu Nhập #%d cho thuốc '%s' (SL: %d %s, Tổng tiền: %,.0f VND).",
                warehouseStaff.getFullName(), savedSlip.getId(), product.getName(), request.getQuantity(), product.getUnit(), totalAmount);

        Notification notification = new Notification(
                warehouseStaff,
                admin,
                NotificationType.IMPORT_CONFIRM,
                savedSlip.getId(),
                message
        );
        notificationRepository.save(notification);

        log.info("-> Đã tạo Phiếu Nhập #{} trạng thái PENDING, gửi thông báo duyệt đến Admin.", savedSlip.getId());
        return WarehouseSlipResponse.fromEntity(savedSlip);
    }

    /**
     * Nghiệp vụ 2: Lập Phiếu Hủy (DISPOSAL)
     */
    @Transactional
    public WarehouseSlipResponse createDisposalSlip(WarehouseSlipRequest request, User warehouseStaff) {
        if (request.getBatchId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn lô hàng cần hủy!");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Số lượng hủy phải lớn hơn 0!");
        }

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy lô hàng với ID: " + request.getBatchId()));

        String reason = request.getReason();
        if (!StringUtils.hasText(reason)) {
            reason = Boolean.TRUE.equals(batch.getIsDamaged()) ? "Bị hỏng trong quá trình lưu kho" :
                    (batch.getExpDate().isBefore(LocalDate.now()) ? "Đã hết hạn sử dụng" : "Yêu cầu hủy kho định kỳ");
        }
        reason = reason.trim();

        Product product = batch.getProduct();
        BigDecimal importPrice = batch.getImportPrice();
        BigDecimal totalAmount = importPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        User admin = userRepository.findByRoleName(RoleName.ROLE_ADMIN).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản Quản trị viên/Chủ cửa hàng!"));

        Warehouse warehouse = batch.getWarehouse();
        if (warehouse == null && request.getWarehouseId() != null) {
            warehouse = warehouseRepository.findById(request.getWarehouseId()).orElse(null);
        }
        if (warehouse == null) {
            warehouse = warehouseRepository.findAll().stream().findFirst().orElse(null);
        }

        // ĐIỀU KIỆN KIỂM DUYỆT BAN ĐẦU:
        // Không ràng buộc lô hết hạn hay hỏng theo yêu cầu người dùng, chỉ cần không hủy quá số lượng tồn kho của lô
        boolean isQuantityExceeded = request.getQuantity() > batch.getQuantity();

        if (isQuantityExceeded) {
            WarehouseSlip failedSlip = new WarehouseSlip(
                    SlipType.DISPOSAL,
                    warehouseStaff,
                    warehouse,
                    null,
                    totalAmount
            );
            failedSlip.setDisposalDate(java.time.LocalDateTime.now());
            failedSlip.setStatus(SlipStatus.REJECTED);
            failedSlip.setResultState("fail");
            WarehouseSlipItem item = new WarehouseSlipItem(
                    failedSlip,
                    product,
                    batch,
                    request.getQuantity(),
                    importPrice,
                    reason
            );
            failedSlip.addItem(item);
            WarehouseSlip savedFailed = warehouseSlipRepository.save(failedSlip);
            log.warn("-> Lập phiếu hủy THẤT BẠI: Số lượng hủy ({}) vượt tồn kho lô ({}).", request.getQuantity(), batch.getQuantity());
            return WarehouseSlipResponse.fromEntity(savedFailed);
        }

        // Thỏa mãn điều kiện sơ bộ -> Lưu phiếu ở trạng thái PENDING
        WarehouseSlip slip = new WarehouseSlip(
                SlipType.DISPOSAL,
                warehouseStaff,
                warehouse,
                null,
                totalAmount
        );
        slip.setDisposalDate(java.time.LocalDateTime.now());
        WarehouseSlipItem item = new WarehouseSlipItem(
                slip,
                product,
                batch,
                request.getQuantity(),
                importPrice,
                reason
        );
        slip.addItem(item);
        WarehouseSlip savedSlip = warehouseSlipRepository.save(slip);

        // Gửi thông báo đến Admin
        String reasonText = reason;
        String message = String.format("NV Kho %s yêu cầu duyệt Phiếu Hủy #%d cho Lô #%d của thuốc '%s' (Lý do: %s, SL hủy: %d %s).",
                warehouseStaff.getFullName(), savedSlip.getId(), batch.getId(), product.getName(), reasonText, request.getQuantity(), product.getUnit());

        Notification notification = new Notification(
                warehouseStaff,
                admin,
                NotificationType.DISPOSAL_CONFIRM,
                savedSlip.getId(),
                message
        );
        notificationRepository.save(notification);

        log.info("-> Đã tạo Phiếu Hủy #{} trạng thái PENDING, gửi thông báo duyệt đến Admin.", savedSlip.getId());
        return WarehouseSlipResponse.fromEntity(savedSlip);
    }

    /**
     * Tìm kiếm các Lô hàng phục vụ chức năng Lập Phiếu Hủy
     */
    @Transactional(readOnly = true)
    public List<BatchResponse> searchBatchesForDisposal(String query) {
        List<Batch> batches;
        if (StringUtils.hasText(query)) {
            batches = batchRepository.searchAvailableBatchesForDisposal(query.trim());
        } else {
            batches = batchRepository.findByStatus(BatchStatus.AVAILABLE);
        }
        return batches.stream()
                .map(BatchResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách Phiếu kho (Nhập / Hủy)
     */
    @Transactional(readOnly = true)
    public List<WarehouseSlipResponse> getAllSlips(String typeFilter, String statusFilter) {
        List<WarehouseSlip> list = warehouseSlipRepository.findAllByOrderByCreatedAtDesc();

        if (StringUtils.hasText(typeFilter) && !"ALL".equalsIgnoreCase(typeFilter)) {
            try {
                SlipType type = SlipType.valueOf(typeFilter.trim());
                list = list.stream().filter(s -> s.getType() == type).collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (StringUtils.hasText(statusFilter) && !"ALL".equalsIgnoreCase(statusFilter)) {
            try {
                SlipStatus status = SlipStatus.valueOf(statusFilter.trim());
                list = list.stream().filter(s -> s.getStatus() == status).collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return list.stream()
                .map(WarehouseSlipResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(WarehouseResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest req) {
        if (!StringUtils.hasText(req.getWarehouseName())) {
            throw new IllegalArgumentException("Tên kho không được để trống!");
        }
        if (warehouseRepository.existsByWarehouseName(req.getWarehouseName().trim())) {
            throw new IllegalArgumentException("Kho '" + req.getWarehouseName() + "' đã tồn tại!");
        }
        Warehouse wh = new Warehouse(req.getWarehouseName().trim(), req.getWarehouseAddress() != null ? req.getWarehouseAddress().trim() : "");
        Warehouse saved = warehouseRepository.save(wh);
        return WarehouseResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(SupplierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> searchSuppliers(String query) {
        List<Supplier> suppliers;
        if (StringUtils.hasText(query)) {
            suppliers = supplierRepository.searchSuppliers(query.trim());
        } else {
            suppliers = supplierRepository.findAll();
        }
        return suppliers.stream()
                .map(SupplierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest req) {
        if (!StringUtils.hasText(req.getName())) {
            throw new IllegalArgumentException("Tên nhà cung cấp không được để trống!");
        }
        if (supplierRepository.existsByNameIgnoreCase(req.getName().trim())) {
            throw new IllegalArgumentException("Nhà cung cấp '" + req.getName() + "' đã tồn tại!");
        }
        Supplier sup = new Supplier(
                req.getName().trim(),
                req.getPhone() != null ? req.getPhone().trim() : null,
                req.getRepresentativeName() != null ? req.getRepresentativeName().trim() : null,
                req.getTaxCode() != null ? req.getTaxCode().trim() : null,
                req.getAddress() != null ? req.getAddress().trim() : null
        );
        Supplier saved = supplierRepository.save(sup);
        return SupplierResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> getAllBatches() {
        return batchRepository.findAllByOrderByCreatedAtDesc().stream().map(b -> {
            BatchResponse res = BatchResponse.fromEntity(b);
            if (b.getProduct() != null) {
                res.setProductTotalStock(b.getProduct().getTotalStock());
            }
            return res;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BatchResponse getBatchById(Long id) {
        Batch b = batchRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy lô hàng với ID: " + id));
        BatchResponse res = BatchResponse.fromEntity(b);
        if (b.getProduct() != null) {
            res.setProductTotalStock(b.getProduct().getTotalStock());
        }
        return res;
    }
}
