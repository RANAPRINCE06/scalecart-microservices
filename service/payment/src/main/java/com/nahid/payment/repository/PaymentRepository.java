package com.nahid.payment.repository;

import com.nahid.payment.entity.Payment;
import com.nahid.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {


    Optional<Payment> findByOrderId(UUID orderId);
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            PaymentStatus status
    );

    List<Payment> findByAmountBetweenOrderByCreatedAtDesc(
            BigDecimal minAmount,
            BigDecimal maxAmount
    );

    boolean existsByOrderId(UUID orderId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.userId = :userId AND p.status = :status")
    BigDecimal getTotalAmountByUserAndStatus(
            @Param("userId") Long userId,
            @Param("status") PaymentStatus status
    );


    @Query("SELECT p FROM Payment p WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Payment> findRecentPayments(@Param("since") LocalDateTime since);

    @Query("""
        SELECT p FROM Payment p
        WHERE p.status = 'FAILED'
        AND p.createdAt >= :since
        AND p.failureReason IS NOT NULL
        ORDER BY p.createdAt DESC
    """)
    List<Payment> findFailedPaymentsForRetry(@Param("since") LocalDateTime since);
}