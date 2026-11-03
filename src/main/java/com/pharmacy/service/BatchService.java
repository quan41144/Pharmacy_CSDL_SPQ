package com.pharmacy.service;

import com.pharmacy.dto.BatchInfoDTO;
import com.pharmacy.entity.Batch;
import com.pharmacy.entity.Inventory;
import com.pharmacy.repository.BatchRepository;
import com.pharmacy.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<BatchInfoDTO> getBatchesForSaleFEFO(Long warehouseId, Long productId) {
        List<Batch> batches = batchRepository.findAvailableBatchesForSaleFEFO(warehouseId, productId, LocalDate.now());
        List<BatchInfoDTO> result = new ArrayList<>();

        for (Batch b : batches) {
            Inventory inv = inventoryRepository.findByWarehouseWarehouseIdAndBatchBatchId(warehouseId, b.getBatchId()).orElse(null);
            if (inv != null && inv.getQuantity() > 0) {
                result.add(BatchInfoDTO.builder()
                        .batchId(b.getBatchId())
                        .productId(productId)
                        .batchCode(b.getBatchCode())
                        .mfgDate(b.getMfgDate())
                        .expDate(b.getExpDate())
                        .quantity(inv.getQuantity())
                        .warehouseId(warehouseId)
                        .warehouseName(inv.getWarehouse().getWarehouseName())
                        .status("VALID")
                        .build());
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<BatchInfoDTO> searchBatchesForDisposal(String keyword, Long warehouseId) {
        List<Batch> batches = batchRepository.searchBatchesForDisposal(keyword, warehouseId);
        List<BatchInfoDTO> result = new ArrayList<>();

        for (Batch b : batches) {
            List<Inventory> inventories = inventoryRepository.findByBatchBatchId(b.getBatchId());
            for (Inventory inv : inventories) {
                if (warehouseId == null || inv.getWarehouse().getWarehouseId().equals(warehouseId)) {
                    if (inv.getQuantity() > 0) {
                        String status;
                        if (b.getExpDate().isBefore(LocalDate.now()) || b.getExpDate().isEqual(LocalDate.now())) {
                            status = "Hết hạn";
                        } else if (b.getExpDate().isBefore(LocalDate.now().plusDays(30))) {
                            status = "Gần hết hạn";
                        } else {
                            status = "Còn hạn";
                        }

                        result.add(BatchInfoDTO.builder()
                                .batchId(b.getBatchId())
                                .productId(b.getProduct().getProductId())
                                .batchCode(b.getBatchCode())
                                .mfgDate(b.getMfgDate())
                                .expDate(b.getExpDate())
                                .quantity(inv.getQuantity())
                                .warehouseId(inv.getWarehouse().getWarehouseId())
                                .warehouseName(inv.getWarehouse().getWarehouseName())
                                .status(status)
                                .build());
                    }
                }
            }
        }
        return result;
    }
}
