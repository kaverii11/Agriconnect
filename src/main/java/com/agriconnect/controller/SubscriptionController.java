package com.agriconnect.controller;

import com.agriconnect.model.SubscriptionBox;
import com.agriconnect.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * PART 3: CONTROLLER LAYER - Subscription Management
 * ============================================================
 *
 * MVC ROLE: Controller
 * Handles subscription lifecycle operations.
 *
 * DESIGN PATTERN: FACTORY METHOD (Module 4 Integration)
 * - The request specifies "VEGGIE" or "FRUIT".
 * - The controller delegates to the service, which uses the factory
 *   to instantiate the correct subclass of SubscriptionBox.
 * - This allows the API to be generic and easily extendable for new types.
 */
@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Create a new subscription box.
     * Demonstrates Factory Method support.
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
    public ResponseEntity<List<SubscriptionBox>> listMyBoxes(@PathVariable Long consumerId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByConsumer(consumerId));
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
