package com.agriconnect.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * PART 1: MODEL - GroupOrder (Observer Pattern Subject)
 * ============================================================
 *
 * A GroupOrder is a collective purchase pool where multiple
 * Consumers contribute quantity until the MOQ is reached.
 *
 * DESIGN PATTERN: Observer (Subject side)
 *   - GroupOrder acts as the Subject (Observable).
 *   - It holds a list of OrderObserver (interface).
 *   - When poolTotalQuantity >= targetMinimumOrder,
 *     it calls notifyObservers() which fires the update()
 *     method on every registered observer.
 *   See: GroupOrderSubject.java for the full pattern implementation.
 *
 * DESIGN PRINCIPLE: SRP
 *   - This entity is ONLY responsible for holding order state.
 *   - Payment logic lives in PaymentTransaction (separate class),
 *     satisfying the Single Responsibility Principle.
 *
 * JPA ASSOCIATIONS:
 *   @ManyToOne  GroupOrder references ONE HarvestBatch
 *   @ManyToMany GroupOrder has MANY participating Consumers
 *   @OneToMany  GroupOrder can produce MANY PaymentTransactions
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "group_orders")
public class GroupOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "pool_total_quantity", nullable = false)
    private Double poolTotalQuantity = 0.0;    // Current pooled quantity

    @Column(name = "target_minimum_order", nullable = false)
    private Double targetMinimumOrder;          // MOQ to activate the order

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // -------------------------------------------------------
    // JPA Relationships
    // -------------------------------------------------------

    /** Many GroupOrders can reference the same HarvestBatch. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private HarvestBatch harvestBatch;

    /**
     * Many Consumers can join Many GroupOrders.
     * JPA manages a join table: consumer_group_orders.
     */
    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "consumer_group_orders",
        joinColumns = @JoinColumn(name = "order_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<Consumer> participants = new ArrayList<>();

    /**
     * A GroupOrder can generate multiple payment transactions
     * (one per participating consumer once MOQ is confirmed).
     * SRP: PaymentTransaction is a SEPARATE entity, not embedded here.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "groupOrder", cascade = CascadeType.ALL)
    private List<PaymentTransaction> transactions = new ArrayList<>();

    // -------------------------------------------------------
    // Business logic — encapsulated, controlled access
    // -------------------------------------------------------

    /** Adds a consumer contribution to the pool. */
    public void addContribution(Consumer consumer, double quantity) {
        this.poolTotalQuantity += quantity;
        if (!this.participants.contains(consumer)) {
            this.participants.add(consumer);
        }
    }

    /** Checks if the minimum order quantity has been reached. */
    public boolean isMoqReached() {
        return poolTotalQuantity >= targetMinimumOrder;
    }

    // -------------------------------------------------------
    // Enum — Status lifecycle
    // -------------------------------------------------------
    public enum OrderStatus {
        OPEN,       // Accepting contributions
        CONFIRMED,  // MOQ reached — order locked
        CANCELLED,  // Not enough orders in time
        FULFILLED   // Delivered
    }
}
