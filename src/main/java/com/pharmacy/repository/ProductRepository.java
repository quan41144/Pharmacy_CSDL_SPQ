package com.pharmacy.repository;

import com.pharmacy.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.uses) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)")
    Page<Product> searchProducts(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           ":keyword IS NULL OR :keyword = '' OR " +
           "LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.uses) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.dosage) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.category.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY p.productId ASC")
    List<Product> searchByNameContainingIgnoreCase(@Param("keyword") String keyword);
}
