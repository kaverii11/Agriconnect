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

import com.agriconnect.repository.ConsumerRepository;

/**
 * ============================================================
 * PART 2: DESIGN PATTERN - Facade (Module 3)
 * ============================================================
 */
@Service
public class CheckoutFacade {

    private static final Logger log = LoggerFactory.getLogger(CheckoutFacade.class);

    // Subsystem dependencies — injected by Spring (DIP at repository level)
    private final HarvestBatchRepository        harvestBatchRepository;
    private final DeliverySlotRepository        deliverySlotRepository;
    private final PaymentTransactionRepository  paymentTransactionRepository;
    private final GroupOrderRepository          groupOrderRepository;
    private final IConsumerDeliveryService      consumerDeliveryService;
    private final ConsumerRepository            consumerRepository;

    public CheckoutFacade(
            HarvestBatchRepository harvestBatchRepository,
            DeliverySlotRepository deliverySlotRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            GroupOrderRepository groupOrderRepository,
            IConsumerDeliveryService consumerDeliveryService,
            ConsumerRepository consumerRepository) {
        this.harvestBatchRepository       = harvestBatchRepository;
        this.deliverySlotRepository       = deliverySlotRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.groupOrderRepository         = groupOrderRepository;
        this.consumerDeliveryService      = consumerDeliveryService;
        this.consumerRepository           = consumerRepository;
    }

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
        DeliverySlot slot = consumerDeliveryService.bookDeliverySlot(slotId, consumer.getUserId());
        log.info("Facade Step 2 ✅ Delivery slot #{} locked at {}", slot.getSlotId(), slot.getSlotTime());
        
        // ---- STEP 2.5: Wallet Deduction ----
        if ("Wallet".equalsIgnoreCase(paymentMethod)) {
            if (consumer.getWalletBalance().compareTo(paymentAmount) < 0) {
                throw new IllegalStateException("Insufficient wallet balance.");
            }
            consumer.setWalletBalance(consumer.getWalletBalance().subtract(paymentAmount));
            consumerRepository.save(consumer);
            log.info("Facade Step 2.5 ✅ Deducted amount {} from wallet.", paymentAmount);
        }

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
