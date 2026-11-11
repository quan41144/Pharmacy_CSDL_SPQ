package com.pharmacy.service;

import com.pharmacy.dto.WarehouseDTO;
import com.pharmacy.entity.Warehouse;
import com.pharmacy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final EmployeeRepository employeeRepository;
    private final InvoiceRepository invoiceRepository;
    private final ImportSlipRepository importSlipRepository;
    private final DisposalSlipRepository disposalSlipRepository;

    @Transactional(readOnly = true)
    public List<WarehouseDTO> getAllWarehouses() {
        return warehouseRepository.findAllByOrderByWarehouseIdAsc().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<WarehouseDTO> getActiveWarehouses() {
        return warehouseRepository.findByStatusOrderByWarehouseIdAsc("ACTIVE").stream().map(this::toDTO).toList();
    }

    @Transactional
    public WarehouseDTO saveWarehouse(WarehouseDTO dto) {
        Warehouse warehouse;
        if (dto.getWarehouseId() != null) {
            warehouse = warehouseRepository.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy kho hàng"));
        } else {
            warehouse = new Warehouse();
            warehouse.setStatus("ACTIVE");
        }

        // Cập nhật tên kho: chỉ cập nhật nếu truyền giá trị không rỗng, nếu tạo mới thì bắt buộc
        if (dto.getWarehouseName() != null && !dto.getWarehouseName().trim().isEmpty()) {
            warehouse.setWarehouseName(dto.getWarehouseName().trim());
        } else if (dto.getWarehouseId() == null) {
            throw new RuntimeException("Tên kho không được để trống");
        }

        // Cập nhật địa chỉ kho: chỉ cập nhật nếu truyền giá trị không rỗng, nếu tạo mới thì bắt buộc
        if (dto.getWarehouseAddress() != null && !dto.getWarehouseAddress().trim().isEmpty()) {
            warehouse.setWarehouseAddress(dto.getWarehouseAddress().trim());
        } else if (dto.getWarehouseId() == null) {
            throw new RuntimeException("Địa chỉ kho không được để trống");
        }

        // Cập nhật trạng thái nếu có truyền
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            warehouse.setStatus(dto.getStatus().trim());
        }

        Warehouse saved = warehouseRepository.save(warehouse);
        return toDTO(saved);
    }

    @Transactional
    public WarehouseDTO toggleWarehouseStatus(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kho hàng"));
        String newStatus = "ACTIVE".equalsIgnoreCase(warehouse.getStatus()) ? "INACTIVE" : "ACTIVE";
        warehouse.setStatus(newStatus);
        Warehouse saved = warehouseRepository.save(warehouse);
        return toDTO(saved);
    }

    @Transactional
    public void deleteWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kho hàng"));

        if (invoiceRepository.existsByWarehouseId(warehouseId)) {
            throw new RuntimeException("Không thể xóa kho đã phát sinh hóa đơn bán hàng. Bạn có thể chuyển trạng thái kho sang INACTIVE.");
        }

        if (importSlipRepository.existsByWarehouseId(warehouseId)) {
            throw new RuntimeException("Không thể xóa kho đã phát sinh phiếu nhập hàng. Bạn có thể chuyển trạng thái kho sang INACTIVE.");
        }

        if (disposalSlipRepository.existsByWarehouseId(warehouseId)) {
            throw new RuntimeException("Không thể xóa kho đã phát sinh phiếu hủy hàng. Bạn có thể chuyển trạng thái kho sang INACTIVE.");
        }

        if (employeeRepository.existsByWarehouseId(warehouseId)) {
            throw new RuntimeException("Không thể xóa kho khi vẫn còn nhân viên được phân công làm việc tại kho này.");
        }

        if (inventoryRepository.existsStockInWarehouse(warehouseId)) {
            throw new RuntimeException("Không thể xóa kho khi vẫn còn hàng tồn kho. Vui lòng xuất hoặc hủy hết tồn kho trước khi xóa.");
        }

        // Xóa các dòng tồn kho rỗng (quantity <= 0) nếu có của kho này
        inventoryRepository.deleteByWarehouseId(warehouseId);

        warehouseRepository.delete(warehouse);
    }

    private WarehouseDTO toDTO(Warehouse w) {
        return WarehouseDTO.builder()
                .warehouseId(w.getWarehouseId())
                .warehouseName(w.getWarehouseName())
                .warehouseAddress(w.getWarehouseAddress())
                .status(w.getStatus())
                .build();
    }
}