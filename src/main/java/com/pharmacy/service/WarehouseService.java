package com.pharmacy.service;

import com.pharmacy.dto.WarehouseDTO;
import com.pharmacy.entity.Warehouse;
import com.pharmacy.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Transactional(readOnly = true)
    public List<WarehouseDTO> getAllWarehouses() {
        return warehouseRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<WarehouseDTO> getActiveWarehouses() {
        return warehouseRepository.findByStatus("ACTIVE").stream().map(this::toDTO).toList();
    }

    @Transactional
    public WarehouseDTO saveWarehouse(WarehouseDTO dto) {
        Warehouse warehouse;
        if (dto.getWarehouseId() != null) {
            warehouse = warehouseRepository.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy kho"));
        } else {
            warehouse = new Warehouse();
            warehouse.setStatus("ACTIVE");
        }

        warehouse.setWarehouseName(dto.getWarehouseName());
        warehouse.setWarehouseAddress(dto.getWarehouseAddress());
        if (dto.getStatus() != null) {
            warehouse.setStatus(dto.getStatus());
        }

        Warehouse saved = warehouseRepository.save(warehouse);
        return toDTO(saved);
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
