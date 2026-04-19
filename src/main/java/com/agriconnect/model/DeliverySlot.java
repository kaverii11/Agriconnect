package com.agriconnect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ============================================================
 * PART 1: MODEL - DeliverySlot
 * ============================================================
 *
 * Represents a time-slot for delivery, managed by a
 * LogisticsCoordinator and bookable by a Consumer.
 *
 * DESIGN PRINCIPLE: ISP (Interface Segregation Principle)
 *   The DeliverySlot entity itself is neutral. Two separate
 *   service interfaces expose differentiated operations:
 *     - IConsumerDeliveryService  → book(), cancelBooking()
 *     - ILogisticsAdminService    → createSlot(), closeSlot(), viewAllBookings()
 *   Consumers are never handed an interface with coordinator-
 *   level powers they don't need (ISP).
 *
 * JPA:
 *   @ManyToOne  → Each slot is assigned to one LogisticsCoordinator
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "delivery_slots")
public class DeliverySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "slot_time", nullable = false)
    private LocalDateTime slotTime;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;       // Max bookings allowed

    @Column(name = "current_bookings")
    private Integer currentBookings = 0;

    @Column(name = "zone")
    private String zone;               // Delivery zone/area

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_status")
    private SlotStatus slotStatus = SlotStatus.OPEN;

    /**
     * JPA: @ManyToOne — Each slot is managed by one coordinator.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinator_id")
    private LogisticsCoordinator coordinator;

    // -------------------------------------------------------
    // Business logic (Encapsulation)
    // -------------------------------------------------------

    /** Returns true if there is still capacity available. */
    public boolean isAvailable() {
        return slotStatus == SlotStatus.OPEN && currentBookings < maxCapacity;
    }

    /** Books one slot — thread-safe locking would be added in production. */
    public void book() {
        if (!isAvailable()) {
            throw new IllegalStateException("Delivery slot " + slotId + " is fully booked.");
        }
        this.currentBookings++;
        if (this.currentBookings >= this.maxCapacity) {
            this.slotStatus = SlotStatus.FULL;
        }
    }

    /** Cancels one booking, reopening capacity. */
    public void cancelBooking() {
        if (this.currentBookings > 0) {
            this.currentBookings--;
            this.slotStatus = SlotStatus.OPEN;
        }
    }

    public enum SlotStatus {
        OPEN, FULL, CLOSED
    }
}
