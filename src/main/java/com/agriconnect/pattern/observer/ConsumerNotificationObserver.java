package com.agriconnect.pattern.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ============================================================
 * PART 2: DESIGN PATTERN - Observer: Concrete Observer 1
 * ============================================================
 *
 * ConsumerNotificationObserver — sends an in-app notification
 * to all consumers when their group order is confirmed.
 *
 * In a production system this would integrate with Firebase FCM,
 * WebSocket push, or an email queuing service.
 * Here it demonstrates the Observer pattern structure cleanly.
 *
 * OCP + Observer: Adding a new observer type (e.g., FarmerObserver,
 * SMSObserver) requires ONLY creating a new class. No existing
 * Subject or Observer code is modified.
 */
@Component
public class ConsumerNotificationObserver implements OrderObserver {

    private static final Logger log = LoggerFactory.getLogger(ConsumerNotificationObserver.class);

    /**
     * Called by GroupOrderSubject.notifyObservers() when MOQ is reached.
     *
     * @param orderId the confirmed order's ID
     * @param message the notification message to deliver
     */
    @Override
    public void update(Long orderId, String message) {
        // In production: look up all consumers in this order and send push/email.
        log.info("[CONSUMER NOTIFICATION] Order #{} → {}", orderId, message);
        // e.g.: notificationService.sendPushToOrderParticipants(orderId, message);
    }
}
