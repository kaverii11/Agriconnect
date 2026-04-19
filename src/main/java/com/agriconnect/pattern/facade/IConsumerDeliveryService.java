package com.agriconnect.pattern.facade;

import com.agriconnect.model.DeliverySlot;

/**
 * ============================================================
 * PART 2: ISP - Interface for CONSUMER delivery operations
 * ============================================================
 *
 * DESIGN PRINCIPLE: Interface Segregation Principle (ISP)
 *   "Clients should not be forced to depend on interfaces they
 *    do not use." — Robert C. Martin
 *
 * A Consumer ONLY needs to: book a slot, cancel, and view available slots.
 * They should NEVER see createSlot() or closeSlot() — those are
 * coordinator operations. By splitting into two interfaces:
 *   1. IConsumerDeliveryService  → narrow, consumer-facing
 *   2. ILogisticsAdminService    → wide, coordinator-facing
 * ...we ensure consumers are never given coordinator-level power.
 *
 * This reduces coupling and makes the Consumer controller
 * completely free of logistics-admin code.
 */
public interface IConsumerDeliveryService {

    /**
     * Consumer books an available delivery slot for their order.
     *
     * @param slotId     the slot to book
     * @param consumerId the booking consumer
     * @return the updated DeliverySlot
     */
    DeliverySlot bookDeliverySlot(Long slotId, Long consumerId);

    /**
     * Consumer cancels their delivery slot booking.
     */
    DeliverySlot cancelDeliveryBooking(Long slotId, Long consumerId);

    /**
     * Consumer views all open delivery slots for a given zone.
     */
    java.util.List<DeliverySlot> getAvailableSlotsForZone(String zone);
}
