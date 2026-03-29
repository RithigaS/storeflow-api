package com.grootan.storeflow.specification;

import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.enums.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    //  Existing method (keep as-is, don't break old code)
    public static Specification<Product> withFilters(
            String category,
            ProductStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
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
            //  ignore soft deleted
            predicates = cb.and(predicates, cb.isNull(root.get("deletedAt")));

            return predicates;
        };
    }

    // advanced search (with name)
    public static Specification<Product> withFiltersAndName(
            String name,
            String category,
            ProductStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            //  name search (LIKE, case-insensitive)
            if (name != null && !name.isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(cb.lower(root.get("name")),
                                "%" + name.trim().toLowerCase() + "%"));
            }

            //  category filter
            if (category != null && !category.isBlank()) {
                predicates = cb.and(predicates,
                        cb.equal(cb.lower(root.get("category").get("name")),
                                category.trim().toLowerCase()));
            }

            //  status filter
            if (status != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("status"), status));
            }

            //  price range
            if (minPrice != null) {
                predicates = cb.and(predicates,
                        cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates = cb.and(predicates,
                        cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            //  ignore soft deleted
            predicates = cb.and(predicates,
                    cb.isNull(root.get("deletedAt")));

            return predicates;
        };
    }
}