package com.agriconnect.pattern.facade;

import com.agriconnect.model.DeliverySlot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================
 * PART 2: ISP - Interface for LOGISTICS COORDINATOR operations
 * ============================================================
 *
 * DESIGN PRINCIPLE: ISP (Interface Segregation Principle)
 *   This interface is BROADER than IConsumerDeliveryService.
 *   It includes methods that ONLY a LogisticsCoordinator should
 *   access: creating, closing, and inspecting all bookings.
 *
 *   If we had a single fat interface with ALL methods, consumers
 *   would be forced to implement/depend on createSlot() and
 *   closeSlot() — methods irrelevant to them. ISP prevents this.
 *
 *   The implementing service (LogisticsService) can implement BOTH
 *   interfaces, providing full functionality to coordinators while
 *   consumers only see the narrow IConsumerDeliveryService.
 */
public interface ILogisticsAdminService extends IConsumerDeliveryService {

    /**
     * COORDINATOR ONLY: Create a new delivery slot in the system.
     */
    DeliverySlot createSlot(LocalDateTime slotTime, int maxCapacity,
                            String zone, Long coordinatorId);

    /**
     * COORDINATOR ONLY: Close a slot and prevent further bookings.
     */
    DeliverySlot closeSlot(Long slotId);

    /**
     * COORDINATOR ONLY: View all bookings across all slots (admin view).
     */
    List<DeliverySlot> getAllSlotsForCoordinator(Long coordinatorId);
}
