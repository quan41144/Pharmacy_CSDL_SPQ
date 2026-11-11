package com.pharmacy.service;

import com.pharmacy.dto.CreateDisposalSlipRequest;
import com.pharmacy.dto.DisposalSlipItemRequest;
import com.pharmacy.dto.DisposalSlipItemResponseDTO;
import com.pharmacy.dto.DisposalSlipResponseDTO;
import com.pharmacy.entity.*;
import com.pharmacy.repository.*;
import com.pharmacy.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DisposalSlipService {

    private final DisposalSlipRepository disposalSlipRepository;
    private final DisposalSlipItemRepository disposalSlipItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final EmployeeRepository employeeRepository;
    private final BatchRepository batchRepository;
    private final InventoryRepository inventoryRepository;
    private final JdbcTemplate jdbcTemplate;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Transactional
    public DisposalSlipResponseDTO createDisposalSlip(CreateDisposalSlipRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new RuntimeException("Chưa đăng nhập");
        }

        Employee staff = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên kho"));

        if (!"ACTIVE".equalsIgnoreCase(staff.getWorkStatus())) {
            throw new RuntimeException("Nhân viên đã ngừng làm việc, không được lập phiếu hủy.");
        }

        if (!"ROLE_WAREHOUSE".equalsIgnoreCase(staff.getPosition()) && !"ROLE_ADMIN".equalsIgnoreCase(staff.getPosition())) {
            throw new RuntimeException("Nhân viên bán hàng không được hủy hàng. Chỉ nhân viên kho hoặc quản trị viên mới được lập phiếu hủy.");
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kho"));

        if (!"ACTIVE".equalsIgnoreCase(warehouse.getStatus())) {
            throw new RuntimeException("Kho đã ngừng hoạt động, không được thao tác.");
        }

        List<DisposalSlipItemRequest> itemsReq = request.getItems();
        if (itemsReq == null || itemsReq.isEmpty()) {
            if (request.getBatchId() != null) {
                itemsReq = List.of(DisposalSlipItemRequest.builder()
                        .batchId(request.getBatchId())
                        .quantity(request.getQuantity())
                        .reason(request.getReason())
                        .build());
            } else {
                throw new RuntimeException("Phiếu hủy phải có ít nhất một lô hàng cần hủy");
            }
        }

        // Validate all items
        Map<Long, Integer> batchTotalQty = new LinkedHashMap<>();
        for (DisposalSlipItemRequest it : itemsReq) {
            if (it.getBatchId() == null) {
                throw new RuntimeException("Mã lô không được để trống");
            }
            if (it.getQuantity() == null || it.getQuantity() <= 0) {
                throw new RuntimeException("Số lượng hủy phải lớn hơn 0");
            }
            if (it.getReason() == null || it.getReason().trim().isEmpty()) {
                throw new RuntimeException("Lý do hủy thuốc là bắt buộc");
            }
            batchTotalQty.merge(it.getBatchId(), it.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : batchTotalQty.entrySet()) {
            Long bId = entry.getKey();
            int totalReqQty = entry.getValue();

            Inventory inventory = inventoryRepository.findByWarehouseWarehouseIdAndBatchBatchId(request.getWarehouseId(), bId)
                    .orElseThrow(() -> new RuntimeException("Lô thuốc #" + bId + " không tồn tại trong kho " + warehouse.getWarehouseName()));

            if (inventory.getQuantity() < totalReqQty) {
                Batch b = batchRepository.findById(bId).orElse(null);
                String batchLabel = (b != null) ? (b.getBatchCode() + " - " + (b.getProduct() != null ? b.getProduct().getProductName() : "")) : ("#" + bId);
                throw new RuntimeException("Số lượng hủy đề xuất (" + totalReqQty + ") vượt quá tồn kho hiện tại (" + inventory.getQuantity() + ") của lô " + batchLabel);
            }
        }

        DisposalSlip slip = DisposalSlip.builder()
                .warehouseStaff(staff)
                .warehouse(warehouse)
                .status("PENDING")
                .build();

        DisposalSlip savedSlip = disposalSlipRepository.saveAndFlush(slip);

        Map<Long, DisposalSlipItem> itemMap = new LinkedHashMap<>();
        for (DisposalSlipItemRequest it : itemsReq) {
            Batch batch = batchRepository.findById(it.getBatchId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lô hàng #" + it.getBatchId()));

            Long bId = batch.getBatchId();
            if (itemMap.containsKey(bId)) {
                DisposalSlipItem existing = itemMap.get(bId);
                existing.setQuantity(existing.getQuantity() + it.getQuantity());
                existing.setReason(existing.getReason() + "; " + it.getReason().trim());
            } else {
                DisposalSlipItemId itemId = DisposalSlipItemId.builder()
                        .disposalSlipId(savedSlip.getDisposalSlipId())
                        .batchId(bId)
                        .build();

                DisposalSlipItem item = DisposalSlipItem.builder()
                        .id(itemId)
                        .disposalSlip(savedSlip)
                        .batch(batch)
                        .quantity(it.getQuantity())
                        .reason(it.getReason().trim())
                        .build();

                itemMap.put(bId, item);
            }
        }

        disposalSlipItemRepository.saveAll(itemMap.values());
        disposalSlipItemRepository.flush();

        return toDTO(savedSlip);
    }

    @Transactional
    public DisposalSlipResponseDTO confirmDisposalSlip(Long disposalSlipId) {
        try {
            jdbcTemplate.execute("CALL sp_confirm_disposal_slip(" + disposalSlipId + ")");
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
            throw new RuntimeException("Xác nhận hủy thất bại: " + msg, e);
        }

        DisposalSlip updated = disposalSlipRepository.findById(disposalSlipId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu hủy"));
        return toDTO(updated);
    }

    @Transactional
    public DisposalSlipResponseDTO cancelDisposalSlip(Long disposalSlipId) {
        DisposalSlip slip = disposalSlipRepository.findById(disposalSlipId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu hủy"));

        if (!"PENDING".equalsIgnoreCase(slip.getStatus())) {
            throw new RuntimeException("Chỉ được hủy/từ chối phiếu hủy khi đang ở trạng thái PENDING");
        }

        slip.setStatus("CANCELLED");
        DisposalSlip saved = disposalSlipRepository.save(slip);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<DisposalSlipResponseDTO> getAllDisposalSlips() {
        return disposalSlipRepository.findAllByOrderByDisposalSlipIdDesc().stream().map(this::toDTO).toList();
    }

    public DisposalSlipResponseDTO toDTO(DisposalSlip slip) {
        List<DisposalSlipItemResponseDTO> itemDTOs = new ArrayList<>();
        List<DisposalSlipItem> items = (slip.getItems() != null && !slip.getItems().isEmpty())
                ? slip.getItems()
                : disposalSlipItemRepository.findByDisposalSlipDisposalSlipId(slip.getDisposalSlipId());

        if (items != null) {
            for (DisposalSlipItem it : items) {
                itemDTOs.add(DisposalSlipItemResponseDTO.builder()
                        .batchId(it.getBatch().getBatchId())
                        .batchCode(it.getBatch().getBatchCode())
                        .productId(it.getBatch().getProduct().getProductId())
                        .productName(it.getBatch().getProduct().getProductName())
                        .unit(it.getBatch().getProduct().getUnit())
                        .mfgDate(it.getBatch().getMfgDate())
                        .expDate(it.getBatch().getExpDate())
                        .quantity(it.getQuantity())
                        .reason(it.getReason())
                        .build());
            }
        }

        return DisposalSlipResponseDTO.builder()
                .disposalSlipId(slip.getDisposalSlipId())
                .warehouseStaffId(slip.getWarehouseStaff().getEmployeeId())
                .warehouseStaffName(slip.getWarehouseStaff().getFullName())
                .warehouseId(slip.getWarehouse().getWarehouseId())
                .warehouseName(slip.getWarehouse().getWarehouseName())
                .disposalDate(slip.getDisposalDate())
                .status(slip.getStatus())
                .items(itemDTOs)
                .build();
    }
}
