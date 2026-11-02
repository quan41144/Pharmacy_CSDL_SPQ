package com.pharmacy.medicare.controller;

import com.pharmacy.medicare.dto.ApiResponse;
import com.pharmacy.medicare.dto.product.*;
import com.pharmacy.medicare.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1", "/api"})
public class ProductPublicController {

    private final ProductService productService;

    public ProductPublicController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "false") Boolean includeInactive) {
        List<ProductResponse> list;
        if (Boolean.TRUE.equals(includeInactive)) {
            list = productService.getAllProductsAdmin(true);
        } else {
            list = productService.searchProducts(keyword, categoryId);
        }
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/products/live-search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> liveSearch(
            @RequestParam(name = "q", required = false, defaultValue = "") String query) {
        List<ProductResponse> list = productService.liveSearch(query);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse res = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @GetMapping("/products/check-name")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkProductName(
            @RequestParam String name,
            @RequestParam(required = false) Long excludeId) {
        boolean exists = productService.checkProductNameExists(name, excludeId);
        String message = exists ? "Tên thuốc đã tồn tại!" : "Tên thuốc hợp lệ.";
        return ResponseEntity.ok(ApiResponse.success(message, Map.of(
                "exists", exists,
                "message", message
        )));
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse res = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success("Thêm thuốc mới vào hệ thống thành công!", res));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @RequestBody UpdateProductRequest request) {
        ProductResponse res = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thuốc thành công!", res));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> deleteProduct(@PathVariable Long id) {
        ProductResponse res = productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Đã ngừng kinh doanh thuốc thành công (Soft Delete)!", res));
    }

    @PostMapping("/products/upload-image")
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProductImage(
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file hình ảnh!");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "png";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }

        String savedFilename = "prod_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
        Path targetDir = Paths.get("uploads/products");
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Path targetPath = targetDir.resolve(savedFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String imageUrl = "/uploads/products/" + savedFilename;
        return ResponseEntity.ok(ApiResponse.success("Tải ảnh thuốc thành công!", Map.of("imageUrl", imageUrl)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        List<CategoryResponse> list = productService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse res = productService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse res = productService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success("Thêm nhóm thuốc thành công!", res));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse res = productService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật nhóm thuốc thành công!", res));
    }

    @GetMapping("/sales-staff")
    public ResponseEntity<ApiResponse<List<SalesStaffResponse>>> getSalesStaffList() {
        List<SalesStaffResponse> list = productService.getSalesStaffList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách nhân viên bán hàng thành công", list));
    }
}
