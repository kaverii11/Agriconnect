package com.agriconnect.service;

import com.agriconnect.model.Consumer;
import com.agriconnect.model.SubscriptionBox;
import com.agriconnect.pattern.dip.INotificationService;
import com.agriconnect.pattern.factory.SubscriptionFactory;
import com.agriconnect.repository.ConsumerRepository;
import com.agriconnect.repository.SubscriptionBoxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * ============================================================
 * SERVICE LAYER - Module 4: Subscriptions & Analytics
 * ============================================================
 *
 * MVC ROLE: Business logic layer for subscription management.
 *
 * DESIGN PATTERN: Factory Method (used here as Client)
 *   SubscriptionService holds a reference to SubscriptionFactory
 *   (the interface). It calls factory.createSubscription(...)
 *   without knowing the concrete class being instantiated.
 *   Swapping AgriBoxFactory for another factory requires ZERO
 *   changes to this class.
 *
 * DESIGN PRINCIPLE: DIP (Dependency Inversion Principle)
 *   SubscriptionSchedulerService (high-level) depends on:
 *     → INotificationService  (abstraction)     ← injected
 *     → SubscriptionFactory   (abstraction)     ← injected
 *   It does NOT depend on:
 *     ✗ EmailNotificationService (concrete)
 *     ✗ AgriBoxFactory           (concrete)
 *   Both high and low-level modules depend on the abstraction. ✅
 */
@Service
@Transactional
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    /**
     * DIP: This field is typed to the INTERFACE, not the
     * concrete EmailNotificationService. The high-level service
     * doesn't know (or care) how notifications are delivered.
     */
    private final INotificationService     notificationService; // DIP ✅

    /**
     * DIP: Typed to SubscriptionFactory interface.
     * AgriBoxFactory is the concrete implementation injected
     * by Spring, but SubscriptionService never references it.
     */
    private final SubscriptionFactory      subscriptionFactory; // DIP + Factory ✅

    private final SubscriptionBoxRepository subscriptionBoxRepository;
    private final ConsumerRepository        consumerRepository;

    public SubscriptionService(INotificationService notificationService,
                               SubscriptionFactory subscriptionFactory,
                               SubscriptionBoxRepository subscriptionBoxRepository,
                               ConsumerRepository consumerRepository) {
        // DIP satisfied: receiving abstractions, not concretions
        this.notificationService     = notificationService;
        this.subscriptionFactory     = subscriptionFactory;
        this.subscriptionBoxRepository = subscriptionBoxRepository;
        this.consumerRepository       = consumerRepository;
    }

    // -------------------------------------------------------
    // WEEKLY BOX SUBSCRIPTION (Major Feature — Module 4)
    // -------------------------------------------------------

    /**
     * Creates a new subscription for a consumer.
     *
     * FACTORY METHOD PATTERN:
     *   subscriptionFactory.createSubscription(...) is called here.
     *   The factory decides whether to instantiate VeggieBox or FruitBox.
     *   SubscriptionService is completely decoupled from concrete types.
     *
     * @param consumerId  the subscribing consumer
     * @param boxType     "VEGGIE" or "FRUIT"
     * @param frequency   "WEEKLY", "BIWEEKLY", or "MONTHLY"
     * @param preference  consumer's preference string
     * @return            persisted SubscriptionBox
     */
    public SubscriptionBox subscribe(Long consumerId, String boxType,
                                     String frequency, String preference) {
        Consumer consumer = consumerRepository.findById(consumerId)
            .orElseThrow(() -> new IllegalArgumentException("Consumer not found: " + consumerId));

        // FACTORY METHOD: Delegate object creation to the factory interface ✅
        SubscriptionBox box = subscriptionFactory.createSubscription(
            boxType, consumer, frequency, preference);

        SubscriptionBox saved = subscriptionBoxRepository.save(box);

        // DIP: Use abstraction to notify — not EmailNotificationService directly ✅
        notificationService.sendNotification(
            consumer.getEmail(),
            "AgriConnect — Subscription Confirmed! 🥦",
            "Hello " + consumer.getName() + "! Your " + saved.getBoxDescription() +
            " subscription is active. First delivery: " + saved.getNextDeliveryDate()
        );

        return saved;
    }

    /** Cancel a subscription gracefully. */
    public SubscriptionBox cancelSubscription(Long subscriptionId) {
        SubscriptionBox box = subscriptionBoxRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        box.setActive(false);

        // DIP: Notification via abstraction ✅
        notificationService.sendNotification(
            box.getConsumer().getEmail(),
            "AgriConnect — Subscription Cancelled",
            "Your subscription #" + subscriptionId + " has been cancelled."
        );

        return subscriptionBoxRepository.save(box);
    }

    /** Retrieve all subscriptions for a consumer. */
    @Transactional(readOnly = true)
    public List<SubscriptionBox> getSubscriptionsByConsumer(Long consumerId) {
        return subscriptionBoxRepository.findByConsumer_UserId(consumerId);
    }

    // -------------------------------------------------------
    // SUBSCRIPTION SCHEDULER (Automated renewal + notifications)
    // -------------------------------------------------------

    /**
     * SCHEDULED TASK — runs every day at 8:00 AM.
     * Processes all subscriptions due today and advances them.
     *
     * DIP IN ACTION:
     *   notificationService is the INotificationService abstraction.
     *   Whether this sends an email, SMS, or push notification is
     *   determined entirely by which bean Spring injects (@Primary).
     *   This scheduler method is completely agnostic to delivery channel.
     */
    @Scheduled(cron = "0 0 8 * * ?") // Every day at 08:00
    public void processDailyRenewals() {
        List<SubscriptionBox> dueToday =
            subscriptionBoxRepository.findByActiveTrueAndNextDeliveryDate(LocalDate.now());

        log.info("[SCHEDULER] Processing {} subscription renewals for {}", dueToday.size(), LocalDate.now());

        for (SubscriptionBox box : dueToday) {
            // Advance delivery date to next cycle
            box.scheduleNextDelivery();
            subscriptionBoxRepository.save(box);

            // DIP: Delegate notification to the abstraction ✅
            notificationService.sendNotification(
                box.getConsumer().getEmail(),
                "AgriConnect — Your Box Ships Today! 📦",
                "Hello " + box.getConsumer().getName() + "! " +
                box.getBoxDescription() + " is being prepared for delivery. " +
                "Next scheduled delivery: " + box.getNextDeliveryDate()
            );
        }
    }
}
