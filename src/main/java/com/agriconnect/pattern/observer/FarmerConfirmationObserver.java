package com.agriconnect.pattern.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Concrete Observer 2 — Farmer is also notified when their
 * harvest batch's group order reaches MOQ.
 * Demonstrates that multiple observers can be registered on
 * the same Subject with zero code changes to GroupOrderSubject.
 */
@Component
public class FarmerConfirmationObserver implements OrderObserver {

    private static final Logger log = LoggerFactory.getLogger(FarmerConfirmationObserver.class);

    @Override
    public void update(Long orderId, String message) {
        log.info("[FARMER ALERT] Prepare harvest for Order #{} — {}", orderId, message);
        // In production: trigger farmer dashboard notification & harvest preparation workflow
    }
}
