package com.pharmacy.medicare.service;

import com.pharmacy.medicare.dto.product.*;
import com.pharmacy.medicare.entity.Batch;
import com.pharmacy.medicare.entity.Category;
import com.pharmacy.medicare.entity.Product;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.entity.enums.BatchStatus;
import com.pharmacy.medicare.entity.enums.RoleName;
import com.pharmacy.medicare.repository.BatchRepository;
import com.pharmacy.medicare.repository.CategoryRepository;
import com.pharmacy.medicare.repository.ProductRepository;
import com.pharmacy.medicare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository,
                          BatchRepository batchRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
    }

    private Batch getActiveBatch(Long productId) {
        List<Batch> list = batchRepository.findByProductIdAndStatusOrderByExpDateAsc(productId, BatchStatus.AVAILABLE);
        return list.stream().filter(b -> b.getQuantity() > 0).findFirst().orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> ProductResponse.fromEntity(p, getActiveBatch(p.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProductsAdmin(Boolean includeInactive) {
        return productRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(includeInactive) || Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> ProductResponse.fromEntity(p, getActiveBatch(p.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword, Long categoryId) {
        boolean hasKeyword = StringUtils.hasText(keyword);
        boolean hasCategory = categoryId != null && categoryId > 0;

        List<Product> products;
        if (hasKeyword && hasCategory) {
            products = productRepository.searchByKeywordAndCategory(keyword.trim().toLowerCase(), categoryId);
        } else if (hasKeyword) {
            products = productRepository.searchByKeyword(keyword.trim().toLowerCase());
        } else if (hasCategory) {
            products = productRepository.findByCategoryId(categoryId);
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> ProductResponse.fromEntity(p, getActiveBatch(p.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> liveSearch(String query) {
        if (!StringUtils.hasText(query)) {
            return getAllProducts();
        }
        return productRepository.liveSearchByName(query.trim()).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> ProductResponse.fromEntity(p, getActiveBatch(p.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy sản phẩm với ID: " + id));
        return ProductResponse.fromEntity(product, getActiveBatch(product.getId()));
    }

    @Transactional(readOnly = true)
    public boolean checkProductNameExists(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) return false;
        String cleanName = name.trim();
        if (excludeId != null && excludeId > 0) {
            return productRepository.existsByNameIgnoreCaseAndIdNot(cleanName, excludeId);
        }
        return productRepository.existsByNameIgnoreCase(cleanName);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhóm thuốc ID: " + id));
        return CategoryResponse.fromEntity(cat);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Tên nhóm thuốc không được để trống!");
        }
        if (categoryRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new IllegalArgumentException("Tên nhóm thuốc đã tồn tại!");
        }

        Category category = new Category(request.getName().trim(), request.getDescription() != null ? request.getDescription().trim() : null);
        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhóm thuốc ID: " + id));

        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Tên nhóm thuốc không được để trống!");
        }

        String trimmedName = request.getName().trim();
        if (!trimmedName.equalsIgnoreCase(category.getName()) && categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("Tên nhóm thuốc đã tồn tại!");
        }

        category.setName(trimmedName);
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription().trim());
        }

        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SalesStaffResponse> getSalesStaffList() {
        List<User> staff = userRepository.findByRoleName(RoleName.ROLE_SALES);
        if (staff.isEmpty()) {
            staff = userRepository.findByRoleName(RoleName.ROLE_ADMIN);
        }
        return staff.stream()
                .map(SalesStaffResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Tên thuốc là bắt buộc!");
        }
        if (checkProductNameExists(request.getName().trim(), null)) {
            throw new IllegalArgumentException("Tên thuốc đã tồn tại!");
        }
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Nhóm thuốc là bắt buộc!");
        }
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhóm thuốc ID: " + request.getCategoryId()));

        if (!StringUtils.hasText(request.getDescription())) {
            throw new IllegalArgumentException("Mô tả thuốc là bắt buộc!");
        }
        if (!StringUtils.hasText(request.getUses())) {
            throw new IllegalArgumentException("Công dụng thuốc là bắt buộc!");
        }
        if (!StringUtils.hasText(request.getIngredients())) {
            throw new IllegalArgumentException("Thành phần thuốc là bắt buộc!");
        }
        if (!StringUtils.hasText(request.getUnit())) {
            throw new IllegalArgumentException("Đơn vị tính là bắt buộc!");
        }
        if (!StringUtils.hasText(request.getImageUrl())) {
            throw new IllegalArgumentException("Hình ảnh thuốc là bắt buộc!");
        }

        BigDecimal importPrice = request.getImportPrice() != null ? request.getImportPrice() : new BigDecimal("10000.00");
        BigDecimal sellingPrice = request.getSellingPrice() != null ? request.getSellingPrice() : importPrice;

        Product product = new Product(
                request.getName().trim(),
                category,
                request.getImageUrl().trim(),
                request.getDescription().trim(),
                request.getUses().trim(),
                request.getIngredients().trim(),
                request.getUnit().trim(),
                importPrice,
                sellingPrice,
                0
        );
        product.setDosageForm(StringUtils.hasText(request.getDosageForm()) ? request.getDosageForm().trim() : "Viên nén");
        product.setIsActive(true);

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved, null);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thuốc với ID: " + id));

        if (StringUtils.hasText(request.getName())) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(product.getName()) && checkProductNameExists(newName, id)) {
                throw new IllegalArgumentException("Tên thuốc đã tồn tại!");
            }
            product.setName(newName);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhóm thuốc ID: " + request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getDescription() != null) product.setDescription(request.getDescription().trim());
        if (request.getUses() != null) product.setUses(request.getUses().trim());
        if (request.getIngredients() != null) product.setIngredients(request.getIngredients().trim());
        if (StringUtils.hasText(request.getDosageForm())) product.setDosageForm(request.getDosageForm().trim());
        if (StringUtils.hasText(request.getUnit())) product.setUnit(request.getUnit().trim());
        if (StringUtils.hasText(request.getImageUrl())) product.setImageUrl(request.getImageUrl().trim());
        if (request.getImportPrice() != null) product.setImportPrice(request.getImportPrice());
        if (request.getSellingPrice() != null) product.setSellingPrice(request.getSellingPrice());
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved, getActiveBatch(saved.getId()));
    }

    @Transactional
    public ProductResponse deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thuốc với ID: " + id));
        // Soft delete: chuyển sang trạng thái ngừng kinh doanh
        product.setIsActive(false);
        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved, getActiveBatch(saved.getId()));
    }
}
