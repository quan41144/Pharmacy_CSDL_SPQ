package com.pharmacy.service;

import com.pharmacy.dto.CreateDisposalSlipRequest;
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
import java.util.List;

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
    public DisposalSlipResponseDTO createAndConfirmDisposalSlip(CreateDisposalSlipRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new RuntimeException("Chưa đăng nhập");
        }

        Employee staff = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên kho"));

        if (!"ACTIVE".equalsIgnoreCase(staff.getWorkStatus())) {
            throw new RuntimeException("Nhân viên đã ngừng làm việc, không được lập phiếu hủy.");
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kho"));

        if (!"ACTIVE".equalsIgnoreCase(warehouse.getStatus())) {
            throw new RuntimeException("Kho đã ngừng hoạt động, không được thao tác.");
        }

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lô hàng"));

        Inventory inventory = inventoryRepository.findByWarehouseWarehouseIdAndBatchBatchId(request.getWarehouseId(), request.getBatchId())
                .orElseThrow(() -> new RuntimeException("Lô thuốc không tồn tại trong kho này"));

        if (inventory.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Số lượng hủy (" + request.getQuantity() + ") vượt quá tồn kho hiện tại (" + inventory.getQuantity() + ")");
        }

        DisposalSlip slip = DisposalSlip.builder()
                .warehouseStaff(staff)
                .warehouse(warehouse)
                .status("PENDING")
                .build();

        DisposalSlip savedSlip = disposalSlipRepository.saveAndFlush(slip);

        DisposalSlipItemId itemId = DisposalSlipItemId.builder()
                .disposalSlipId(savedSlip.getDisposalSlipId())
                .batchId(batch.getBatchId())
                .build();

        DisposalSlipItem item = DisposalSlipItem.builder()
                .id(itemId)
                .disposalSlip(savedSlip)
                .batch(batch)
                .quantity(request.getQuantity())
                .reason(request.getReason())
                .build();

        disposalSlipItemRepository.saveAndFlush(item);

        // Gọi Procedure để trừ tồn kho và chuyển trạng thái sang DISPOSED
        try {
            jdbcTemplate.execute("CALL sp_confirm_disposal_slip(" + savedSlip.getDisposalSlipId() + ")");
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

        DisposalSlip updated = disposalSlipRepository.findById(savedSlip.getDisposalSlipId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu hủy"));
        return toDTO(updated);
    }

    @Transactional
    public void confirmDisposalSlip(Long disposalSlipId) {
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
