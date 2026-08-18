package com.nahid.order.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(
        name = "order_idempotency_records",
        indexes = {
                @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true)
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OrderIdempotencyRecord extends BaseEntity<UUID> {

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
