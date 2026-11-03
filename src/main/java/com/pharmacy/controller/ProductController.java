package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.ProductDTO;
import com.pharmacy.dto.ProductDetailDTO;
import com.pharmacy.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("productId").ascending());
        Page<ProductDTO> result = productService.searchProducts(keyword, categoryId, warehouseId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/live-search")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> liveSearch(@RequestParam(defaultValue = "") String keyword) {
        List<ProductDTO> list = productService.liveSearch(keyword);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDTO>> getProductDetail(
            @PathVariable Long id,
            @RequestParam(required = false) Long warehouseId
    ) {
        try {
            ProductDetailDTO detail = productService.getProductDetail(id, warehouseId);
            return ResponseEntity.ok(ApiResponse.ok(detail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> saveProduct(@RequestBody ProductDTO dto) {
        try {
            ProductDTO saved = productService.saveProduct(dto);
            return ResponseEntity.ok(ApiResponse.ok("Lưu thuốc thành công", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(ApiResponse.ok("Xóa thuốc thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
