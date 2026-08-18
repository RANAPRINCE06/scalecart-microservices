package com.nahid.product.service.impl;

import com.nahid.product.dto.response.ProductResponseDto;
import com.nahid.product.entity.Product;
import com.nahid.product.exception.ResourceNotFoundException;
import com.nahid.product.exception.StockUpdateException;
import com.nahid.product.mapper.ProductMapper;
import com.nahid.product.repository.ProductRepository;
import com.nahid.product.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public ProductResponseDto updateStock(Long id, Integer newStock) {
        if (newStock < 0) {
            throw new StockUpdateException("Stock quantity cannot be negative");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        product.setStockQuantity(newStock);
        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public boolean isProductAvailable(Long id, Integer requiredQuantity) {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
            return product.getIsActive() && product.getStockQuantity() >= requiredQuantity;

    }
}
