package com.grootan.storeflow.service.impl;

import com.grootan.storeflow.dto.CreateProductRequest;
import com.grootan.storeflow.dto.ProductDto;
import com.grootan.storeflow.dto.UpdateProductRequest;
import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.exception.AppException;
import com.grootan.storeflow.exception.ResourceNotFoundException;
import com.grootan.storeflow.mapper.ProductMapper;
import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.service.FileStorageService;
import com.grootan.storeflow.service.ProductService;
import com.grootan.storeflow.specification.ProductSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            FileStorageService fileStorageService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public ProductDto create(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setStatus(request.getStockQuantity() == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE);

        return ProductMapper.toDto(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getAll(String category, ProductStatus status, BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        return productRepository.findAll(
                ProductSpecification.withFilters(category, status, minPrice, maxPrice),
                PageRequest.of(page, size)
        ).map(ProductMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return ProductMapper.toDto(product);
    }

    @Override
    public ProductDto update(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setStatus(request.getStockQuantity() == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE);

        return ProductMapper.toDto(productRepository.save(product));
    }

    @Override
    public ProductDto adjustStock(Long id, int delta) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        int updated = product.getStockQuantity() + delta;
        if (updated < 0) {
            throw new AppException("Stock cannot become negative", HttpStatus.BAD_REQUEST);
        }

        product.setStockQuantity(updated);
        product.setStatus(updated == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE);
        return ProductMapper.toDto(productRepository.save(product));
    }

    @Override
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setStatus(ProductStatus.DISCONTINUED);
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    @Override
    public ProductDto uploadProductImage(Long id, MultipartFile file) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        String imageUrl = fileStorageService.storeProductImage(file, id);
        product.setImageUrl(imageUrl);

        return ProductMapper.toDto(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getAllWithPagination(
            String name,
            String category,
            ProductStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sort
    ) {
        //  handle size edge cases
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        //  default sort
        Sort sortObj = Sort.by("createdAt").descending();

        //  parse sort param (example: price,asc)
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0];
            String direction = parts.length > 1 ? parts[1] : "desc";

            sortObj = direction.equalsIgnoreCase("asc")
                    ? Sort.by(field).ascending()
                    : Sort.by(field).descending();
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);

        return productRepository.findAll(
                ProductSpecification.withFiltersAndName(name, category, status, minPrice, maxPrice),
                pageable
        ).map(ProductMapper::toDto);
    }

    @Override
    public List<ProductDto> getAllWithCursor(String name, String category, ProductStatus status, BigDecimal minPrice, BigDecimal maxPrice, Long cursor, int size, String sort) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold)
                .stream()
                .map(ProductMapper::toDto)
                .toList();
    }
}