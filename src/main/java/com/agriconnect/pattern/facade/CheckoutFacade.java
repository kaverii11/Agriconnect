package com.agriconnect.pattern.facade;

import com.agriconnect.model.*;
import com.agriconnect.repository.DeliverySlotRepository;
import com.agriconnect.repository.GroupOrderRepository;
import com.agriconnect.repository.HarvestBatchRepository;
import com.agriconnect.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 * PART 2: DESIGN PATTERN - Facade (Module 3)
 * ============================================================
 *
 * PATTERN: Facade (Structural)
 *   Provides a SIMPLIFIED, unified interface to a complex
 *   subsystem. Clients (Controllers) call ONE method
 *   instead of orchestrating multiple subsystem calls.
 *
 *   WITHOUT Facade — GroupBuyingController would need to:
 *     1. call HarvestBatchRepository to check stock
 *     2. call DeliverySlotRepository to lock the slot
 *     3. call PaymentTransactionRepository to create record
 *     4. call GroupOrderRepository to confirm the order
 *     5. Handle rollback on any failure
 *
 *   WITH Facade — Controller calls ONE method:
 *     checkoutFacade.processCheckout(orderId, slotId, consumerId, amount)
 *
 *   The Facade hides all complexity behind a clean API surface.
 *
 * DESIGN PRINCIPLE: DIP (partial)
 *   CheckoutFacade depends on Spring-injected repository
 *   abstractions (JpaRepository interfaces), not concrete Hibernate
 *   classes directly.
 *
 * ISP SATISFACTION:
 *   CheckoutFacade calls IConsumerDeliveryService (narrow interface)
 *   for slot booking — not the full ILogisticsAdminService.
 */
@Service
public class CheckoutFacade {

    private static final Logger log = LoggerFactory.getLogger(CheckoutFacade.class);

    // Subsystem dependencies — injected by Spring (DIP at repository level)
    private final HarvestBatchRepository        harvestBatchRepository;
    private final DeliverySlotRepository        deliverySlotRepository;
    private final PaymentTransactionRepository  paymentTransactionRepository;
    private final GroupOrderRepository          groupOrderRepository;
    private final IConsumerDeliveryService      consumerDeliveryService; // ISP: narrow interface

    public CheckoutFacade(
            HarvestBatchRepository harvestBatchRepository,
            DeliverySlotRepository deliverySlotRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            GroupOrderRepository groupOrderRepository,
            IConsumerDeliveryService consumerDeliveryService) {
        this.harvestBatchRepository       = harvestBatchRepository;
        this.deliverySlotRepository       = deliverySlotRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.groupOrderRepository         = groupOrderRepository;
        this.consumerDeliveryService      = consumerDeliveryService;
    }

    /**
     * ============================================================
     * THE FACADE METHOD — hides ALL subsystem complexity.
     * This is the single entry point for the entire checkout flow.
     * ============================================================
     *
     * STEP 1: Verify inventory is available in HarvestBatch
     * STEP 2: Lock / book the chosen DeliverySlot
     * STEP 3: Create a PaymentTransaction record
     * STEP 4: Confirm the GroupOrder status
     *
     * The Controller only calls THIS method.
     * It knows nothing about the subsystem internals — Facade achieved.
     *
     * @param groupOrderId  the confirmed group order to checkout
     * @param slotId        the delivery slot the consumer chose
     * @param consumer      the Consumer making the purchase
     * @param paymentAmount the total amount to be charged
     * @param paymentMethod e.g. "UPI", "CARD"
     * @return              the created PaymentTransaction
     */
    @Transactional
    public PaymentTransaction processCheckout(Long groupOrderId,
                                              Long slotId,
                                              Consumer consumer,
                                              BigDecimal paymentAmount,
                                              String paymentMethod) {
        log.info("Facade: Beginning checkout for Order #{}, Consumer #{}", groupOrderId, consumer.getUserId());

        // ---- STEP 1: Inventory Verification (Subsystem 1) ----
        GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
            .orElseThrow(() -> new IllegalArgumentException("GroupOrder not found: " + groupOrderId));

        HarvestBatch batch = groupOrder.getHarvestBatch();
        if (batch.isExpired()) {
            throw new IllegalStateException("Cannot checkout — HarvestBatch has expired.");
        }
        if (batch.getAvailableQuantity() < groupOrder.getPoolTotalQuantity()) {
            throw new IllegalStateException("Inventory shortfall — batch cannot fulfil this order.");
        }
        log.info("Facade Step 1 ✅ Inventory verified for batch #{}", batch.getBatchId());

        // ---- STEP 2: Delivery Slot Locking (Subsystem 2) ----
        // Uses the ISP-compliant narrow interface — consumer cannot call createSlot()
        DeliverySlot slot = consumerDeliveryService.bookDeliverySlot(slotId, consumer.getUserId());
        log.info("Facade Step 2 ✅ Delivery slot #{} locked at {}", slot.getSlotId(), slot.getSlotTime());

        // ---- STEP 3: Payment Processing (Subsystem 3) ----
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTotalAmount(paymentAmount);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setPaymentStatus(PaymentTransaction.PaymentStatus.SUCCESS);
        transaction.setGroupOrder(groupOrder);
        transaction.setConsumer(consumer);
        transaction.setTransactionTime(LocalDateTime.now());
        paymentTransactionRepository.save(transaction);
        log.info("Facade Step 3 ✅ Payment of ₹{} recorded ({})", paymentAmount, paymentMethod);

        // ---- STEP 4: Confirm Order (Subsystem 4) ----
        groupOrder.setStatus(GroupOrder.OrderStatus.FULFILLED);
        batch.addQuantitySold(groupOrder.getPoolTotalQuantity());
        groupOrderRepository.save(groupOrder);
        harvestBatchRepository.save(batch);
        log.info("Facade Step 4 ✅ Order #{} fulfilled. Inventory updated.", groupOrderId);

        return transaction;
    }
}
