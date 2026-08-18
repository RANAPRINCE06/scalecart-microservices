package com.nahid.notification.repository;

import com.nahid.notification.entity.Notification;
import com.nahid.notification.enums.NotificationStatus;
import com.nahid.notification.enums.ReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {


    List<Notification> findByUserIdOrderByCreatedAtDesc(String customerId);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String customerId, Pageable pageable);

    List<Notification> findByStatusOrderByCreatedAtDesc(NotificationStatus status);



    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < 3 ORDER BY n.createdAt ASC")
    List<Notification> findFailedNotificationsForRetry(@Param("status") NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<Notification> findNotificationsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    boolean existsByReferenceIdAndReferenceType(UUID referenceId, ReferenceType referenceType);
}
