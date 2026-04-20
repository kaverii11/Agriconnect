package com.agriconnect.controller;

import com.agriconnect.model.SubscriptionBox;
import com.agriconnect.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================
 * CONTROLLER LAYER - Subscription Management
 * ============================================================
 * MVC ROLE: Controller — exposes subscription lifecycle endpoints.
 * DESIGN PATTERN: Factory Method (via SubscriptionService/SubscriptionFactory)
 */
@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final jakarta.persistence.EntityManager entityManager;

    public SubscriptionController(SubscriptionService subscriptionService,
                                   jakarta.persistence.EntityManager entityManager) {
        this.subscriptionService = subscriptionService;
        this.entityManager = entityManager;
    }

    // -------------------------------------------------------
    // DB MAINTENANCE
    // -------------------------------------------------------

    /** One-time fix for legacy discriminator values */
    @PostMapping("/fix-db")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<String> fixDB() {
        entityManager.createNativeQuery(
            "UPDATE subscription_boxes SET box_type='VEGGIE_BOX' WHERE box_type='VeggieBox'")
            .executeUpdate();
        entityManager.createNativeQuery(
            "UPDATE subscription_boxes SET box_type='FRUIT_BOX' WHERE box_type='FruitBox'")
            .executeUpdate();
        return ResponseEntity.ok("Discriminator values fixed.");
    }

    // -------------------------------------------------------
    // CREATE
    // -------------------------------------------------------

    /**
     * Create a new subscription box.
     * Supports optional pickupArea and pickupTimeSlot query params.
     */
    @PostMapping("/{consumerId}/create")
    public ResponseEntity<SubscriptionBox> createBox(
            @PathVariable Long consumerId,
            @RequestParam String boxType,
            @RequestParam String frequency,
            @RequestParam String preference,
            @RequestParam(required = false) String pickupArea,
            @RequestParam(required = false) String pickupTimeSlot) {
        SubscriptionBox box = subscriptionService.subscribe(
            consumerId, boxType, frequency, preference, pickupArea, pickupTimeSlot);
        return ResponseEntity.ok(box);
    }

    // -------------------------------------------------------
    // LIST
    // -------------------------------------------------------

    /** List consumer's active subscriptions as safe DTOs. */
    @GetMapping("/{consumerId}/my-boxes")
    public ResponseEntity<?> listMyBoxes(@PathVariable Long consumerId) {
        try {
            List<SubscriptionBox> boxes = subscriptionService.getSubscriptionsByConsumer(consumerId);
            List<java.util.Map<String, Object>> result = boxes.stream().map(box -> {
                SubscriptionBox actualBox = (SubscriptionBox) org.hibernate.Hibernate.unproxy(box);
                String boxType = actualBox instanceof com.agriconnect.model.VeggieBox ? "VEGGIE" : "FRUIT";
                String pref = "";
                if (boxType.equals("VEGGIE")) {
                    pref = ((com.agriconnect.model.VeggieBox) actualBox).getVeggiePreference();
                } else {
                    pref = ((com.agriconnect.model.FruitBox) actualBox).getFruitPreference();
                }
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("subscriptionId", actualBox.getSubscriptionId());
                map.put("frequency", actualBox.getFrequency());
                map.put("nextDeliveryDate", actualBox.getNextDeliveryDate());
                map.put("active", actualBox.isActive());
                map.put("pricePerCycle", actualBox.getPricePerCycle());
                map.put("boxType", boxType);
                map.put("veggiePreference", pref);
                map.put("fruitPreference", pref);
                map.put("boxDescription", actualBox.getBoxDescription());
                map.put("pickupArea", actualBox.getPickupArea());
                map.put("pickupTimeSlot", actualBox.getPickupTimeSlot());
                map.put("pausedUntil", actualBox.getPausedUntil());
                map.put("pausedReason", actualBox.getPausedReason());
                map.put("paused", actualBox.isPaused());
                map.put("totalDeliveriesCompleted", actualBox.getTotalDeliveriesCompleted() != null ? actualBox.getTotalDeliveriesCompleted() : 0);
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.status(500).body(sw.toString());
        }
    }

    // -------------------------------------------------------
    // UPDATE PICKUP PREFERENCES
    // -------------------------------------------------------

    @PatchMapping("/{subscriptionId}/pickup")
    public ResponseEntity<String> updatePickup(
            @PathVariable Long subscriptionId,
            @RequestParam String pickupArea,
            @RequestParam String pickupTimeSlot) {
        subscriptionService.updatePickupPreferences(subscriptionId, pickupArea, pickupTimeSlot);
        return ResponseEntity.ok("Pickup preferences updated successfully.");
    }

    // -------------------------------------------------------
    // PAUSE / RESUME
    // -------------------------------------------------------

    @PostMapping("/{subscriptionId}/pause")
    public ResponseEntity<String> pauseSubscription(
            @PathVariable Long subscriptionId,
            @RequestParam String pauseUntil,
            @RequestParam(required = false, defaultValue = "Vacation / Travel") String reason) {
        LocalDate until = LocalDate.parse(pauseUntil);
        subscriptionService.pauseSubscription(subscriptionId, until, reason);
        return ResponseEntity.ok("Subscription paused until " + pauseUntil);
    }

    @PostMapping("/{subscriptionId}/resume")
    public ResponseEntity<String> resumeSubscription(@PathVariable Long subscriptionId) {
        subscriptionService.resumeSubscription(subscriptionId);
        return ResponseEntity.ok("Subscription resumed successfully.");
    }

    // -------------------------------------------------------
    // CANCEL
    // -------------------------------------------------------

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<String> cancel(@PathVariable Long subscriptionId) {
        subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok("Subscription cancelled successfully.");
    }
}
