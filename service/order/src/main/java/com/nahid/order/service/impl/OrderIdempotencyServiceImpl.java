package com.nahid.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahid.order.dto.request.CreateOrderRequest;
import com.nahid.order.dto.request.OrderDto;
import com.nahid.order.entity.Order;
import com.nahid.order.entity.OrderIdempotencyRecord;
import com.nahid.order.exception.IdempotencyConflictException;
import com.nahid.order.mapper.OrderMapper;
import com.nahid.order.repository.OrderIdempotencyRepository;
import com.nahid.order.repository.OrderRepository;
import com.nahid.order.service.OrderIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderIdempotencyServiceImpl implements OrderIdempotencyService {

    private final OrderIdempotencyRepository idempotencyRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String computeRequestHash(CreateOrderRequest request) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(jsonPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        } catch (Exception e) {
            log.error("Failed to compute request hash for order request", e);
            throw new IllegalArgumentException("Invalid request payload for hashing", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderDto> getExistingOrder(String idempotencyKey, String currentRequestHash) {
        Optional<OrderIdempotencyRecord> optionalRecord = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        if (optionalRecord.isEmpty()) {
            return Optional.empty();
        }

        OrderIdempotencyRecord record = optionalRecord.get();
        if (!record.getRequestHash().equals(currentRequestHash)) {
            log.warn("Idempotency conflict for key {}: existing hash {}, new hash {}",
                    idempotencyKey, record.getRequestHash(), currentRequestHash);
            throw new IdempotencyConflictException(
                    "Idempotency-Key '" + idempotencyKey + "' has already been used for a different request payload."
            );
        }

        if ("IN_PROGRESS".equals(record.getStatus())) {
            log.warn("Concurrent/duplicate request in progress for key {}", idempotencyKey);
            throw new IdempotencyConflictException(
                    "An order creation request with Idempotency-Key '" + idempotencyKey + "' is currently in progress. Please retry shortly."
            );
        }

        if ("COMPLETED".equals(record.getStatus()) && record.getOrderId() != null) {
            log.info("Idempotency hit for key {}: returning existing order ID {}", idempotencyKey, record.getOrderId());
            Optional<Order> existingOrder = orderRepository.findById(record.getOrderId());
            return existingOrder.map(orderMapper::toDto);
        }

        return Optional.empty();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderIdempotencyRecord createInProcessRecord(String idempotencyKey, String requestHash) {
        try {
            OrderIdempotencyRecord record = OrderIdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .requestHash(requestHash)
                    .status("IN_PROGRESS")
                    .build();

            return idempotencyRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent database insert violation for idempotency key {}", idempotencyKey);
            throw new IdempotencyConflictException(
                    "Idempotency-Key '" + idempotencyKey + "' is already registered or processing in parallel.", e
            );
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(OrderIdempotencyRecord record, OrderDto orderDto) {
        if (record == null || record.getId() == null) {
            return;
        }
        idempotencyRepository.findById(record.getId()).ifPresent(rec -> {
            rec.setOrderId(orderDto.getOrderId());
            rec.setOrderNumber(orderDto.getOrderNumber());
            rec.setStatus("COMPLETED");
            idempotencyRepository.save(rec);
            log.info("Successfully completed idempotency record for key {} with order ID {}", rec.getIdempotencyKey(), orderDto.getOrderId());
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(OrderIdempotencyRecord record) {
        if (record == null || record.getId() == null) {
            return;
        }
        try {
            idempotencyRepository.deleteById(record.getId());
            log.info("Removed idempotency record for key {} after creation failure to allow retry", record.getIdempotencyKey());
        } catch (Exception e) {
            log.error("Failed to delete failed idempotency record {}", record.getIdempotencyKey(), e);
        }
    }
}
