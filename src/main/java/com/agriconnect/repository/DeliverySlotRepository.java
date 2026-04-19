package com.agriconnect.repository;

import com.agriconnect.model.DeliverySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliverySlotRepository extends JpaRepository<DeliverySlot, Long> {

    /** Find all OPEN slots in a specific delivery zone. */
    List<DeliverySlot> findByZoneAndSlotStatus(String zone, DeliverySlot.SlotStatus status);

    /** All slots managed by a specific coordinator. */
    List<DeliverySlot> findByCoordinator_UserId(Long coordinatorId);
}
