package com.pharmacy.service;

import com.pharmacy.dto.BatchInfoDTO;
import com.pharmacy.dto.ProductDTO;
import com.pharmacy.dto.ProductDetailDTO;
import com.pharmacy.entity.Batch;
import com.pharmacy.entity.Category;
import com.pharmacy.entity.Inventory;
import com.pharmacy.entity.Product;
import com.pharmacy.repository.BatchRepository;
import com.pharmacy.repository.CategoryRepository;
import com.pharmacy.repository.InventoryRepository;
import com.pharmacy.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BatchRepository batchRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String keyword, Long categoryId, Long warehouseId, Pageable pageable) {
        Page<Product> page = productRepository.searchProducts(keyword, categoryId, pageable);
        return page.map(p -> toDTO(p, warehouseId));
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> liveSearch(String keyword) {
        List<Product> products = productRepository.searchByNameContainingIgnoreCase(keyword);
        return products.stream().map(p -> toDTO(p, null)).toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetail(Long productId, Long warehouseId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + productId));

        List<Batch> batches = batchRepository.findByProductProductId(productId);
        List<BatchInfoDTO> batchDTOs = new ArrayList<>();

        for (Batch b : batches) {
            List<Inventory> inventories = inventoryRepository.findByBatchBatchId(b.getBatchId());
            for (Inventory inv : inventories) {
                if (warehouseId == null || inv.getWarehouse().getWarehouseId().equals(warehouseId)) {
                    String status;
                    if (b.getExpDate().isBefore(LocalDate.now()) || b.getExpDate().isEqual(LocalDate.now())) {
                        status = "EXPIRED";
                    } else if (b.getExpDate().isBefore(LocalDate.now().plusDays(30))) {
                        status = "NEAR_EXPIRY";
                    } else {
                        status = "VALID";
                    }

                    batchDTOs.add(BatchInfoDTO.builder()
                            .batchId(b.getBatchId())
                            .productId(product.getProductId())
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

        return ProductDetailDTO.builder()
                .product(toDTO(product, warehouseId))
                .batches(batchDTOs)
                .build();
    }

    @Transactional
    public ProductDTO saveProduct(ProductDTO dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục nhóm thuốc"));

        Product product;
        if (dto.getProductId() != null) {
            product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc cần cập nhật"));
        } else {
            product = new Product();
        }

        product.setCategory(category);
        product.setProductName(dto.getProductName());
        product.setDosage(dto.getDosage());
        product.setUses(dto.getUses());
        product.setDosageForm(dto.getDosageForm());
        product.setUnit(dto.getUnit());
        product.setSellingPrice(dto.getSellingPrice());
        product.setImageUrl(dto.getImageUrl());

        Product saved = productRepository.save(product);
        return toDTO(saved, null);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Thuốc không tồn tại");
        }
        productRepository.deleteById(productId);
    }

    public ProductDTO toDTO(Product p, Long warehouseId) {
        Integer totalStock;
        if (warehouseId != null) {
            totalStock = inventoryRepository.getTotalStockByWarehouseAndProduct(warehouseId, p.getProductId());
        } else {
            totalStock = inventoryRepository.getTotalStockByProduct(p.getProductId());
        }

        return ProductDTO.builder()
                .productId(p.getProductId())
                .categoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getCategoryName() : null)
                .productName(p.getProductName())
                .dosage(p.getDosage())
                .uses(p.getUses())
                .dosageForm(p.getDosageForm())
                .unit(p.getUnit())
                .sellingPrice(p.getSellingPrice())
                .imageUrl(p.getImageUrl())
                .totalStock(totalStock != null ? totalStock : 0)
                .build();
    }
}
