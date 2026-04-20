package com.agriconnect.controller;

import com.agriconnect.model.SubscriptionBox;
import com.agriconnect.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================
 * PART 3: CONTROLLER LAYER - Subscription Management
 * ============================================================
 */
@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final jakarta.persistence.EntityManager entityManager;

    public SubscriptionController(SubscriptionService subscriptionService, jakarta.persistence.EntityManager entityManager) {
        this.subscriptionService = subscriptionService;
        this.entityManager = entityManager;
    }

    @PostMapping("/fix-db")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<String> fixDB() {
        entityManager.createNativeQuery("UPDATE subscription_boxes SET box_type='VEGGIE_BOX' WHERE box_type='VeggieBox'").executeUpdate();
        return ResponseEntity.ok("FIXED DISCRIMINATOR VALUES IN DB!");
    }

    /**
     * Create a new subscription box.
     */
    @PostMapping("/{consumerId}/create")
    public ResponseEntity<SubscriptionBox> createBox(@PathVariable Long consumerId,
                                                    @RequestParam String boxType,
                                                    @RequestParam String frequency,
                                                    @RequestParam String preference) {
        SubscriptionBox box = subscriptionService.subscribe(consumerId, boxType, frequency, preference);
        return ResponseEntity.ok(box);
    }

    /**
     * List consumer's active subscriptions.
     */
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
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.status(500).body(sw.toString());
        }
    }

    /**
     * Cancel a recurring box.
     */
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<String> cancel(@PathVariable Long subscriptionId) {
        subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok("Subscription cancelled successfully.");
    }
}
