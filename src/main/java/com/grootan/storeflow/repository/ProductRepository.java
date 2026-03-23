package com.grootan.storeflow.repository;

import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySkuIgnoreCase(String sku);
    List<Product> findByCategory(Category category);
    List<Product> findAllByStockQuantityLessThan(int threshold);
    boolean existsBySkuIgnoreCase(String sku);
}