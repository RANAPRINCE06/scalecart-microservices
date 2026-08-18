package com.nahid.product.repository;

import com.nahid.product.entity.InventoryReservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<InventoryReservation> findByOrderReference(String orderReference);
}
