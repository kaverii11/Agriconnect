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
 * SERVICE LAYER - Subscription Management
 *
 * DESIGN PATTERN: Factory Method — subscriptionFactory.createSubscription(...)
 * DESIGN PRINCIPLE: DIP — depends on INotificationService abstraction
 */
@Service
@Transactional
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final INotificationService     notificationService;
    private final SubscriptionFactory      subscriptionFactory;
    private final SubscriptionBoxRepository subscriptionBoxRepository;
    private final ConsumerRepository        consumerRepository;

    public SubscriptionService(INotificationService notificationService,
                               SubscriptionFactory subscriptionFactory,
                               SubscriptionBoxRepository subscriptionBoxRepository,
                               ConsumerRepository consumerRepository) {
        this.notificationService      = notificationService;
        this.subscriptionFactory      = subscriptionFactory;
        this.subscriptionBoxRepository = subscriptionBoxRepository;
        this.consumerRepository        = consumerRepository;
    }

    // -------------------------------------------------------
    // CREATE SUBSCRIPTION
    // -------------------------------------------------------

    public SubscriptionBox subscribe(Long consumerId, String boxType,
                                     String frequency, String preference) {
        return subscribe(consumerId, boxType, frequency, preference, null, null);
    }

    public SubscriptionBox subscribe(Long consumerId, String boxType, String frequency,
                                     String preference, String pickupArea, String pickupTimeSlot) {
        Consumer consumer = consumerRepository.findById(consumerId)
            .orElseThrow(() -> new IllegalArgumentException("Consumer not found: " + consumerId));

        SubscriptionBox box = subscriptionFactory.createSubscription(
            boxType, consumer, frequency, preference);

        if (pickupArea != null && !pickupArea.isBlank()) {
            box.setPickupArea(pickupArea);
        }
        if (pickupTimeSlot != null && !pickupTimeSlot.isBlank()) {
            box.setPickupTimeSlot(pickupTimeSlot);
        }

        SubscriptionBox saved = subscriptionBoxRepository.save(box);

        notificationService.sendNotification(
            consumer.getEmail(),
            "AgriConnect — Subscription Confirmed! 🥦",
            "Hello " + consumer.getName() + "! Your " + saved.getBoxDescription() +
            " subscription is active. First delivery: " + saved.getNextDeliveryDate() +
            (pickupArea != null ? ". Pickup: " + pickupArea + " (" + pickupTimeSlot + ")" : "")
        );

        return saved;
    }

    // -------------------------------------------------------
    // MANAGE SUBSCRIPTION
    // -------------------------------------------------------

    /** Update pickup area and time slot for an existing subscription. */
    public SubscriptionBox updatePickupPreferences(Long subscriptionId,
                                                   String pickupArea,
                                                   String pickupTimeSlot) {
        SubscriptionBox box = subscriptionBoxRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        box.setPickupArea(pickupArea);
        box.setPickupTimeSlot(pickupTimeSlot);

        notificationService.sendNotification(
            box.getConsumer().getEmail(),
            "AgriConnect — Pickup Preferences Updated",
            "Your subscription pickup has been updated to: " + pickupArea +
            " at " + pickupTimeSlot + " slot."
        );

        return subscriptionBoxRepository.save(box);
    }

    /**
     * Pause a subscription until a given date.
     * Deliveries within the paused window will be skipped by the scheduler.
     */
    public SubscriptionBox pauseSubscription(Long subscriptionId,
                                             LocalDate pauseUntil,
                                             String reason) {
        SubscriptionBox box = subscriptionBoxRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        box.setPausedUntil(pauseUntil);
        box.setPausedReason(reason != null ? reason : "Paused by user");

        notificationService.sendNotification(
            box.getConsumer().getEmail(),
            "AgriConnect — Subscription Paused",
            "Your subscription is paused until " + pauseUntil +
            ". Reason: " + box.getPausedReason() + ". It will resume automatically."
        );

        log.info("[PAUSE] Subscription #{} paused until {} — Reason: {}", subscriptionId, pauseUntil, reason);
        return subscriptionBoxRepository.save(box);
    }

    /** Resume a paused subscription immediately. */
    public SubscriptionBox resumeSubscription(Long subscriptionId) {
        SubscriptionBox box = subscriptionBoxRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

        box.setPausedUntil(null);
        box.setPausedReason(null);

        notificationService.sendNotification(
            box.getConsumer().getEmail(),
            "AgriConnect — Subscription Resumed 🥦",
            "Your " + box.getBoxDescription() + " subscription has been resumed. " +
            "Next delivery: " + box.getNextDeliveryDate()
        );

        log.info("[RESUME] Subscription #{} resumed", subscriptionId);
        return subscriptionBoxRepository.save(box);
    }

    /** Cancel a subscription gracefully. */
    public SubscriptionBox cancelSubscription(Long subscriptionId) {
        SubscriptionBox box = subscriptionBoxRepository.findById(subscriptionId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
        box.setActive(false);

        notificationService.sendNotification(
            box.getConsumer().getEmail(),
            "AgriConnect — Subscription Cancelled",
            "Your subscription #" + subscriptionId + " has been cancelled. " +
            "Total deliveries completed: " + box.getTotalDeliveriesCompleted()
        );

        return subscriptionBoxRepository.save(box);
    }

    /** Retrieve all subscriptions for a consumer. */
    @Transactional(readOnly = true)
    public List<SubscriptionBox> getSubscriptionsByConsumer(Long consumerId) {
        return subscriptionBoxRepository.findByConsumer_UserId(consumerId);
    }

    // -------------------------------------------------------
    // SUBSCRIPTION SCHEDULER
    // -------------------------------------------------------

    /**
     * SCHEDULED TASK — runs every day at 8:00 AM.
     * Skips paused subscriptions automatically.
     * DIP: notificationService is the INotificationService abstraction.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void processDailyRenewals() {
        List<SubscriptionBox> dueToday =
            subscriptionBoxRepository.findByActiveTrueAndNextDeliveryDate(LocalDate.now());

        log.info("[SCHEDULER] Processing {} subscription renewals for {}", dueToday.size(), LocalDate.now());

        for (SubscriptionBox box : dueToday) {
            // Skip paused subscriptions
            if (box.isPaused()) {
                log.info("[SCHEDULER] Skipping subscription #{} — paused until {}", box.getSubscriptionId(), box.getPausedUntil());
                continue;
            }

            box.scheduleNextDelivery();
            subscriptionBoxRepository.save(box);

            String pickupInfo = (box.getPickupArea() != null)
                ? " Pickup: " + box.getPickupArea() + " — " + box.getPickupTimeSlot() + " slot."
                : " Door delivery scheduled.";

            notificationService.sendNotification(
                box.getConsumer().getEmail(),
                "AgriConnect — Your Box Ships Today! 📦",
                "Hello " + box.getConsumer().getName() + "! " +
                box.getBoxDescription() + " is being prepared for delivery." +
                pickupInfo + " Next delivery: " + box.getNextDeliveryDate() +
                ". Deliveries completed: " + box.getTotalDeliveriesCompleted()
            );
        }
    }
}
