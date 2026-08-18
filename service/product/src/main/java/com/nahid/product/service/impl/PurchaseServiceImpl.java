package com.nahid.product.service.impl;

import com.nahid.product.dto.request.PurchaseProductItemDto;
import com.nahid.product.dto.request.PurchaseProductRequestDto;
import com.nahid.product.dto.response.PurchaseProductItemResultDto;
import com.nahid.product.dto.response.PurchaseProductResponseDto;
import com.nahid.product.entity.InventoryReservation;
import com.nahid.product.entity.InventoryReservationItem;
import com.nahid.product.entity.Product;
import com.nahid.product.enums.ReservationStatus;
import com.nahid.product.exception.PurchaseException;
import com.nahid.product.repository.InventoryReservationRepository;
import com.nahid.product.repository.ProductRepository;
import com.nahid.product.service.PurchaseService;
import com.nahid.product.util.constant.ExceptionMessageConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class PurchaseServiceImpl implements PurchaseService {

    private static final int DEFAULT_RESERVATION_MINUTES = 15;

    private final ProductRepository productRepository;
    private final InventoryReservationRepository reservationRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public PurchaseProductResponseDto reserveInventory(PurchaseProductRequestDto request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Reservation request contains no items");
        }

        InventoryReservation reservation = reservationRepository.findByOrderReference(request.getOrderReference())
                .orElseGet(() -> InventoryReservation.createNew(request.getOrderReference()));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return buildReservationResponse(reservation);
        }

        if (reservation.getId() != null
                && reservation.getStatus() == ReservationStatus.RESERVED
                && !reservation.getItems().isEmpty()) {
            return buildReservationResponse(reservation);
        }

        List<Long> productIds = request.getItems().stream()
                .map(PurchaseProductItemDto::getProductId)
                .toList();

        List<Product> products = productIds.isEmpty()
                ? List.of()
                : productRepository.findAllByIdInForUpdate(productIds);

        Map<Long, Product> productsById = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        List<PurchaseProductItemResultDto> itemResults = new ArrayList<>();
        boolean allAvailable = true;

        for (PurchaseProductItemDto item : request.getItems()) {
            Product product = productsById.get(item.getProductId());
            PurchaseProductItemResultDto result = buildResultItem(item, product);
            itemResults.add(result);
            if (!result.isAvailable()) {
                allAvailable = false;
            }
        }

        if (!allAvailable) {
            String unavailableDetails = itemResults.stream()
                    .filter(item -> !item.isAvailable())
                    .map(item -> String.format(
                            "Product %s (ID: %s, SKU: %s) - %s (Requested: %d, Available: %d)",
                            Optional.ofNullable(item.getProductName()).orElse("Unknown"),
                            item.getProductId() != null ? item.getProductId() : 0,
                            Optional.ofNullable(item.getSku()).orElse("N/A"),
                            Optional.ofNullable(item.getMessage()).orElse("Unavailable"),
                            item.getRequestedQuantity(),
                            item.getAvailableQuantity()))
                    .collect(Collectors.joining("; "));

            String errorDetail = unavailableDetails.isEmpty()
                    ? "One or more products are unavailable"
                    : unavailableDetails;

            throw new PurchaseException(String.format(
                    ExceptionMessageConstant.PRODUCT_RESERVATION_FAILED,
                    errorDetail));
        }

        // reserve inventory atomically
        request.getItems().forEach(item -> {
            Product product = productsById.get(item.getProductId());
            int newStock = product.getStockQuantity() - item.getQuantity();
            if (newStock < 0) {
                throw new IllegalStateException("Cannot reserve more stock than available for product: " + product.getName());
            }
            product.setStockQuantity(newStock);
        });
        productRepository.saveAll(products);

        reservation.getItems().clear();
        request.getItems().forEach(item -> {
            Product product = productsById.get(item.getProductId());
            InventoryReservationItem reservationItem = InventoryReservationItem.builder()
                    .productId(product.getId())
                    .reservedQuantity(item.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            reservation.addItem(reservationItem);
        });
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(DEFAULT_RESERVATION_MINUTES));
        reservationRepository.save(reservation);

        return PurchaseProductResponseDto.builder()
                .orderReference(request.getOrderReference())
                .items(itemResults)
                .build();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void confirmReservation(String orderReference) {
        reservationRepository.findByOrderReference(orderReference)
                .ifPresent(reservation -> {
                    if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
                        reservation.setStatus(ReservationStatus.CONFIRMED);
                        reservation.setExpiresAt(null);
                        reservationRepository.save(reservation);
                    }
                });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void releaseReservation(String orderReference) {
        reservationRepository.findByOrderReference(orderReference)
                .ifPresent(reservation -> {
                    if (reservation.getStatus() != ReservationStatus.RESERVED) {
                        return;
                    }
                    List<Long> productIds = reservation.getItems().stream()
                            .map(InventoryReservationItem::getProductId)
                            .toList();
                    if (!productIds.isEmpty()) {
                        List<Product> products = productRepository.findAllByIdInForUpdate(productIds);
                        Map<Long, Product> productsById = products.stream()
                                .collect(Collectors.toMap(Product::getId, product -> product));
                        reservation.getItems().forEach(item -> {
                            Product product = productsById.get(item.getProductId());
                            if (product != null) {
                                product.setStockQuantity(product.getStockQuantity() + item.getReservedQuantity());
                            }
                        });
                        productRepository.saveAll(products);
                    }
                    reservation.setStatus(ReservationStatus.RELEASED);
                    reservation.setExpiresAt(null);
                    reservationRepository.save(reservation);
                });
    }

    private PurchaseProductItemResultDto buildResultItem(PurchaseProductItemDto item, Product product) {
        if (product == null) {
            return PurchaseProductItemResultDto.builder()
                    .productId(item.getProductId())
                    .requestedQuantity(item.getQuantity())
                    .availableQuantity(0)
                    .available(false)
                    .message("Product not found")
                    .build();
        }

        boolean available = Boolean.TRUE.equals(product.getIsActive()) && product.getStockQuantity() >= item.getQuantity();
        return PurchaseProductItemResultDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .requestedQuantity(item.getQuantity())
                .availableQuantity(product.getStockQuantity())
                .price(product.getPrice())
                .available(available)
                .message(available ? "" : (Boolean.TRUE.equals(product.getIsActive()) ? "Insufficient stock available" : "Product is inactive"))
                .build();
    }

    private PurchaseProductResponseDto buildReservationResponse(InventoryReservation reservation) {
        List<Long> productIds = reservation.getItems().stream()
                .map(InventoryReservationItem::getProductId)
                .toList();

        Map<Long, Product> productsById = productIds.isEmpty()
                ? Map.of()
                : productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<PurchaseProductItemResultDto> itemResults = reservation.getItems().stream()
                .map(item -> {
                    Product product = productsById.get(item.getProductId());
                    return PurchaseProductItemResultDto.builder()
                            .productId(item.getProductId())
                            .productName(product != null ? product.getName() : null)
                            .sku(product != null ? product.getSku() : null)
                            .requestedQuantity(item.getReservedQuantity())
                            .availableQuantity(item.getReservedQuantity())
                            .price(item.getUnitPrice())
                            .available(product != null && Boolean.TRUE.equals(product.getIsActive()))
                            .message(product == null ? "Product information not available" : "")
                            .build();
                })
                .toList();

        return PurchaseProductResponseDto.builder()
                .orderReference(reservation.getOrderReference())
                .items(itemResults)
                .build();
    }
}
