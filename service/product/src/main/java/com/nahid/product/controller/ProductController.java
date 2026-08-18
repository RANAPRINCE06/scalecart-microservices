package com.nahid.product.controller;

import com.nahid.product.dto.request.CreateProductRequestDto;
import com.nahid.product.dto.request.PurchaseProductRequestDto;
import com.nahid.product.dto.request.UpdateProductRequestDto;
import com.nahid.product.dto.response.ApiResponse;
import com.nahid.product.dto.response.ProductResponseDto;
import com.nahid.product.dto.response.PurchaseProductResponseDto;
import com.nahid.product.service.ProductService;
import com.nahid.product.util.constant.ApiResponseConstant;
import com.nahid.product.util.constant.AppConstant;
import com.nahid.product.util.helper.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(@Valid @RequestBody CreateProductRequestDto request) {
        ProductResponseDto response = productService.createProduct(request);
        return ApiResponseUtil.success(response, String.format(ApiResponseConstant.CREATE_SUCCESSFUL, AppConstant.PRODUCT), HttpStatus.CREATED
        );
    }

    @PostMapping("/inventory/reservations")
    public ResponseEntity<ApiResponse<PurchaseProductResponseDto>> reserveInventory(@Valid @RequestBody PurchaseProductRequestDto request) {
        PurchaseProductResponseDto response = productService.reserveInventory(request);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.ACTION_SUCCESSFUL, AppConstant.INVENTORY, AppConstant.RESERVED),
                HttpStatus.OK);
    }

    @PostMapping("/inventory/reservations/{orderReference}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmReservation(@PathVariable String orderReference) {
        productService.confirmReservation(orderReference);
        return ApiResponseUtil.success(
                null,
                String.format(
                        ApiResponseConstant.STATUS_UPDATE_SUCCESSFUL,
                        AppConstant.INVENTORY_RESERVATION,
                        AppConstant.CONFIRMED), HttpStatus.OK);
    }

    @PostMapping("/inventory/reservations/{orderReference}/release")
    public ResponseEntity<ApiResponse<Void>> releaseReservation(@PathVariable String orderReference) {
        productService.releaseReservation(orderReference);
        return ApiResponseUtil.success(
                null,
                String.format(
                        ApiResponseConstant.STATUS_UPDATE_SUCCESSFUL,
                        AppConstant.INVENTORY_RESERVATION,
                        AppConstant.RELEASED
                ),
                HttpStatus.OK
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable Long id) {
        ProductResponseDto response = productService.getProductById(id);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.PRODUCT)
        );
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductBySku(@PathVariable String sku) {
        ProductResponseDto response = productService.getProductBySku(sku);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.PRODUCT)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getAllProducts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponseDto> response = productService.getAllProducts(pageable);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.FETCH_ALL_SUCCESSFUL, AppConstant.PRODUCTS)
        );
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getActiveProducts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponseDto> response = productService.getActiveProducts(pageable);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.ACTIVE_PRODUCTS)
        );
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getProductsByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponseDto> response = productService.getProductsByCategory(categoryId, pageable);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.CATEGORY_PRODUCTS)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponseDto> response = productService.searchProducts(name, brand, minPrice, maxPrice, categoryId, pageable);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.PRODUCT_SEARCH_RESULTS)
        );
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getFeaturedProducts() {
        List<ProductResponseDto> response = productService.getFeaturedProducts();
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.FEATURED_PRODUCTS));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getLowStockProducts() {
        List<ProductResponseDto> response = productService.getLowStockProducts();
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.FETCH_SUCCESSFUL, AppConstant.LOW_STOCK_PRODUCTS));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequestDto request) {
        ProductResponseDto response = productService.updateProduct(id, request);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.UPDATE_SUCCESSFUL, AppConstant.PRODUCT));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponseUtil.success(
                null,
                String.format(ApiResponseConstant.DELETE_SUCCESSFUL, AppConstant.PRODUCT),
                HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateStock(
            @PathVariable Long id,
            @RequestParam Integer stock) {
        ProductResponseDto response = productService.updateStock(id, stock);
        return ApiResponseUtil.success(
                response,
                String.format(ApiResponseConstant.UPDATE_SUCCESSFUL, AppConstant.PRODUCT_STOCK));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<Boolean>> checkProductAvailability(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        boolean isAvailable = productService.isProductAvailable(id, quantity);
        return ApiResponseUtil.success(
                isAvailable,
                String.format(ApiResponseConstant.ACTION_SUCCESSFUL, AppConstant.PRODUCT_AVAILABILITY, AppConstant.CHECKED));
    }
}
