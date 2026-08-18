package com.nahid.product.service.impl;

import com.nahid.product.dto.request.CreateProductRequestDto;
import com.nahid.product.dto.request.PurchaseProductRequestDto;
import com.nahid.product.dto.request.UpdateProductRequestDto;
import com.nahid.product.dto.response.ProductResponseDto;
import com.nahid.product.dto.response.PurchaseProductResponseDto;
import com.nahid.product.entity.Category;
import com.nahid.product.entity.Product;
import com.nahid.product.exception.DuplicateResourceException;
import com.nahid.product.exception.ResourceNotFoundException;
import com.nahid.product.mapper.ProductMapper;
import com.nahid.product.repository.CategoryRepository;
import com.nahid.product.repository.ProductRepository;
import com.nahid.product.service.ProductService;
import com.nahid.product.service.InventoryService;
import com.nahid.product.service.PurchaseService;

import com.nahid.product.util.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.nahid.product.util.constant.AppConstant.PRODUCT;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;
    private final PurchaseService purchaseService;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    @Auditable(eventType = "CREATE", entityName = PRODUCT, action = "CREATE_PRODUCT")
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "featuredProducts", allEntries = true)
    })
    public ProductResponseDto createProduct(CreateProductRequestDto request) {

        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product with SKU " + request.getSku() + " already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setIsActive(true);
        product.setImageUrl(request.getImageUrl());

        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);

    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'id:' + #id")
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'sku:' + #sku")
    public ProductResponseDto getProductBySku(String sku) {

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);
        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getActiveProducts(Pageable pageable) {
        Page<Product> products = productRepository.findByIsActiveTrue(pageable);
        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProductsByCategory(Long categoryId, Pageable pageable) {

        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
        }

        Page<Product> products = productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable);
        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> searchProducts(String name, String brand, BigDecimal minPrice,
                                                   BigDecimal maxPrice, Long categoryId, Pageable pageable) {

        Page<Product> products = productRepository.findProductsWithFilters(
                name, brand, minPrice, maxPrice, categoryId, pageable);
        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "featuredProducts")
    public List<ProductResponseDto> getFeaturedProducts() {
        List<Product> products = productRepository.findByIsFeaturedTrue();
        return productMapper.toResponseList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getLowStockProducts() {
        List<Product> products = productRepository.findLowStockProducts();
        return productMapper.toResponseList(products);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    @Auditable(eventType = "UPDATE", entityName = PRODUCT, action = "UPDATE_PRODUCT")
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "featuredProducts", allEntries = true)
    })
    public ProductResponseDto updateProduct(Long id, UpdateProductRequestDto request) {


        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));
            existingProduct.setCategory(category);
        }

        productMapper.updateProductFromRequest(request, existingProduct);

        Product updatedProduct = productRepository.save(existingProduct);

        return productMapper.toResponse(updatedProduct);

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Auditable(eventType = "DELETE", entityName = PRODUCT, action = "DELETE_PRODUCT")
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "featuredProducts", allEntries = true)
    })
    public void deleteProduct(Long id) {

        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with ID: " + id);
        }
        productRepository.deleteById(id);

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Caching(put = {
            @CachePut(value = "products", key = "'id:' + #id"),
            @CachePut(value = "products", key = "'sku:' + #result.sku")
    })
    public ProductResponseDto updateStock(Long id, Integer newStock) {
        return inventoryService.updateStock(id, newStock);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductAvailable(Long id, Integer requiredQuantity) {
        return inventoryService.isProductAvailable(id, requiredQuantity);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PurchaseProductResponseDto reserveInventory(PurchaseProductRequestDto request) {
        return purchaseService.reserveInventory(request);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void confirmReservation(String orderReference) {
        purchaseService.confirmReservation(orderReference);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void releaseReservation(String orderReference) {
        purchaseService.releaseReservation(orderReference);
    }

}
