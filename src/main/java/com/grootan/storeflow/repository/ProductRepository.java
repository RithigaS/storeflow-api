package com.grootan.storeflow.repository;

import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySkuIgnoreCase(String sku);

    List<Product> findByCategory(Category category);

    boolean existsBySkuIgnoreCase(String sku);

    @Query("""
            SELECT p
            FROM Product p
            WHERE p.deletedAt IS NULL
              AND p.stockQuantity < :threshold
            ORDER BY p.stockQuantity ASC, p.id ASC
            """)
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    // old (used by tests)
    List<Product> findAllByStockQuantityLessThan(int threshold);

}