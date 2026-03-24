package com.grootan.storeflow.specification;

import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.enums.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    public static Specification<Product> withFilters(String category, ProductStatus status, BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (category != null && !category.isBlank()) {
                predicates = cb.and(predicates,
                        cb.equal(cb.lower(root.get("category").get("name")), category.trim().toLowerCase()));
            }
            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return predicates;
        };
    }
}