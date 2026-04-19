package com.agriconnect.pattern.observer;

import com.agriconnect.model.GroupOrder;
import com.agriconnect.model.GroupOrder.OrderStatus;
import com.agriconnect.repository.GroupOrderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * PART 2: DESIGN PATTERN - Observer: Subject (Module 2)
 * ============================================================
 *
 * PATTERN: Observer — SUBJECT (Observable) side
 *   GroupOrderSubject is the entity that is being observed.
 *   It maintains a registry of OrderObserver objects and
 *   fires notifyObservers() whenever the MOQ condition is met.
 *
 *   OBSERVER FLOW:
 *   1. Consumer calls addContribution(consumer, qty)
 *   2. The pool total increases.
 *   3. GroupOrderSubject checks: isMoqReached()?
 *   4. If YES → status = CONFIRMED → notifyObservers() fires.
 *   5. Each registered OrderObserver.update() is called.
 *
 * DESIGN PRINCIPLE: SRP (Single Responsibility Principle)
 *   - GroupOrderSubject manages TWO responsibilities:
 *       a) Observer registration / notification (pattern logic)
 *       b) Delegating to GroupOrderRepository for persistence
 *   - GroupOrder (the JPA entity) ONLY holds state — it contains
 *     NO notification code, preserving SRP for the model.
 *   - PaymentTransaction is a fully separate class — paying for
 *     a confirmed order is a distinct responsibility (SRP).
 *
 * WHY NOT EMBED IN GroupOrder?
 *   Mixing notification logic into the JPA entity would violate
 *   SRP (entity would have 2 reasons to change) and would make
 *   unit testing much harder (JPA entities carry transaction context).
 */
@Service
public class GroupOrderSubject {

    // -------------------------------------------------------
    // Observer Registry
    // In-memory list of all registered observers for this subject.
    // In a distributed system, this would be replaced with a
    // message broker (Kafka/RabbitMQ), but the pattern is identical.
    // -------------------------------------------------------
    private final List<OrderObserver> observers = new ArrayList<>();

    private final GroupOrderRepository groupOrderRepository;

    public GroupOrderSubject(GroupOrderRepository groupOrderRepository) {
        this.groupOrderRepository = groupOrderRepository;
    }

    // -------------------------------------------------------
    // Observer Registration (subscribe / unsubscribe)
    // -------------------------------------------------------

    /**
     * Register an observer to receive order-confirmation notifications.
     * OOP: Runtime polymorphism — any class implementing OrderObserver
     * can be registered (ConsumerNotificationObserver, SMSObserver, etc.)
     */
    public void registerObserver(OrderObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /** Remove an observer (e.g., user opts out of notifications). */
    public void removeObserver(OrderObserver observer) {
        observers.remove(observer);
    }

    // -------------------------------------------------------
    // Core: Add Contribution → check MOQ → notify
    // -------------------------------------------------------

    /**
     * Adds a consumer's quantity contribution to the group order pool.
     * After updating the pool, checks if MOQ has been reached.
     * If so, transitions order to CONFIRMED and fires all observers.
     *
     * @param groupOrder the GroupOrder entity to update
     * @param consumer   the contributing Consumer
     * @param quantity   the quantity being contributed
     */
    public GroupOrder contribute(GroupOrder groupOrder,
                                 com.agriconnect.model.Consumer consumer,
                                 double quantity) {
        // Update pool quantity via controlled entity method
        groupOrder.addContribution(consumer, quantity);

        // Check MOQ threshold
        if (groupOrder.isMoqReached() && groupOrder.getStatus() == OrderStatus.OPEN) {
            groupOrder.setStatus(OrderStatus.CONFIRMED);
            groupOrderRepository.save(groupOrder);

            // OBSERVER PATTERN: Notify all registered observers
            String message = String.format(
                "🎉 Group Order #%d is CONFIRMED! MOQ of %.1f kg reached. " +
                "Your order will be processed shortly.",
                groupOrder.getOrderId(),
                groupOrder.getTargetMinimumOrder()
            );
            notifyObservers(groupOrder.getOrderId(), message);
        } else {
            groupOrderRepository.save(groupOrder);
        }

        return groupOrder;
    }

    // -------------------------------------------------------
    // Internal notification dispatch
    // -------------------------------------------------------

    /**
     * Iterates through all registered observers and calls their
     * update() method. This is the core of the Observer pattern.
     *
     * OOP: Polymorphism — each observer may handle the notification
     * differently (email, SMS, push), but they're all called uniformly.
     */
    private void notifyObservers(Long orderId, String message) {
        for (OrderObserver observer : observers) {
            observer.update(orderId, message); // Polymorphic dispatch
        }
    }
}
