package com.agriconnect.pattern.observer;

/**
 * ============================================================
 * PART 2: DESIGN PATTERN - Observer (Module 2)
 * ============================================================
 *
 * PATTERN: Observer (Behavioral)
 *   Defines a one-to-many dependency between objects.
 *   When the Subject (GroupOrderSubject) changes state,
 *   ALL registered observers are automatically notified.
 *
 * This is the OBSERVER interface (the "Listener" side).
 * Each class that wants to be notified when a group order
 * is confirmed must implement this interface.
 *
 * DESIGN PRINCIPLE: SRP
 *   Notification concerns are isolated here. GroupOrder model
 *   only manages pooling state — it does NOT call notify logic.
 *   The GroupOrderSubject service is the dedicated "notifier".
 */
public interface OrderObserver {

    /**
     * Called automatically when the GroupOrder MOQ is reached.
     *
     * @param orderId     the ID of the now-confirmed group order
     * @param message     human-readable notification text
     */
    void update(Long orderId, String message);
}
