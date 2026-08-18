package com.nahid.order.repository;

import com.nahid.order.entity.OrderIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderIdempotencyRepository extends JpaRepository<OrderIdempotencyRecord, UUID> {

    Optional<OrderIdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
}
