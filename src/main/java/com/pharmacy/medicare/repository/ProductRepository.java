package com.pharmacy.medicare.repository;

import com.pharmacy.medicare.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByNameContainingIgnoreCaseAndCategoryId(String name, Long categoryId);

    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.uses) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.ingredients) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.uses) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.ingredients) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByKeywordAndCategory(@Param("keyword") String keyword, @Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.name ASC")
    List<Product> liveSearchByName(@Param("query") String query);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Product> findByIsActiveTrue();
}
