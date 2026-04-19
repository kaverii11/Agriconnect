package com.agriconnect.service;

import com.agriconnect.model.*;
import com.agriconnect.pattern.observer.ConsumerNotificationObserver;
import com.agriconnect.pattern.observer.FarmerConfirmationObserver;
import com.agriconnect.pattern.observer.GroupOrderSubject;
import com.agriconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * ============================================================
 * SERVICE LAYER - Module 2: Consumer & Group Buying Engine
 * ============================================================
 *
 * MVC ROLE: Business logic layer — knows nothing about HTTP or Views.
 *
 * DESIGN PATTERN: Observer (orchestrated here)
 *   GroupBuyingService wires together the Subject (GroupOrderSubject)
 *   and the Observers (ConsumerNotificationObserver, FarmerConfirmationObserver).
 *   When contribute() is called, the Subject automatically notifies
 *   all registered observers if MOQ is reached.
 *
 * DESIGN PRINCIPLE: SRP
 *   - This service handles Group Buying pool logic (contributions, status).
 *   - Payment splitting is a SEPARATE concern handled by splitAndPay().
 *   - GroupOrderSubject handles observer wiring.
 *   Each class has exactly ONE reason to change.
 */
@Service
@Transactional
public class GroupBuyingService {

    private final GroupOrderRepository          groupOrderRepository;
    private final HarvestBatchRepository        harvestBatchRepository;
    private final ConsumerRepository            consumerRepository;
    private final PaymentTransactionRepository  paymentTransactionRepository;

    // OBSERVER PATTERN: Subject injected by Spring.
    // Registrations happen in the constructor.
    private final GroupOrderSubject              groupOrderSubject;

    public GroupBuyingService(
            GroupOrderRepository groupOrderRepository,
            HarvestBatchRepository harvestBatchRepository,
            ConsumerRepository consumerRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            GroupOrderSubject groupOrderSubject,
            ConsumerNotificationObserver consumerObserver,
            FarmerConfirmationObserver farmerObserver) {
        this.groupOrderRepository         = groupOrderRepository;
        this.harvestBatchRepository       = harvestBatchRepository;
        this.consumerRepository           = consumerRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.groupOrderSubject            = groupOrderSubject;

        // Register observers once at service startup
        this.groupOrderSubject.registerObserver(consumerObserver);
        this.groupOrderSubject.registerObserver(farmerObserver);
    }

    // -------------------------------------------------------
    // GROUP BUYING CART (Major Feature — Module 2)
    // -------------------------------------------------------

    /**
     * Opens a new group buying pool for a harvest batch.
     * Any consumer can then join by calling contribute().
     */
    public GroupOrder openGroupOrder(Long batchId, Double targetMoq) {
        HarvestBatch batch = harvestBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("HarvestBatch not found: " + batchId));

        if (batch.isExpired()) {
            throw new IllegalStateException("Cannot open group order for an expired batch.");
        }

        GroupOrder order = new GroupOrder();
        order.setHarvestBatch(batch);
        order.setTargetMinimumOrder(targetMoq);
        order.setStatus(GroupOrder.OrderStatus.OPEN);
        return groupOrderRepository.save(order);
    }

    /**
     * Consumer joins/contributes to an existing open group order.
     *
     * OBSERVER PATTERN triggered here:
     *   groupOrderSubject.contribute() internally calls notifyObservers()
     *   if MOQ is reached, automatically updating all registered observers.
     *
     * @param orderId    the group order to join
     * @param consumerId the contributing consumer
     * @param quantity   their share of the quantity (kg)
     * @return           updated GroupOrder (may now be CONFIRMED)
     */
    public GroupOrder contribute(Long orderId, Long consumerId, double quantity) {
        GroupOrder order = groupOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("GroupOrder not found: " + orderId));

        if (order.getStatus() != GroupOrder.OrderStatus.OPEN) {
            throw new IllegalStateException("Order #" + orderId + " is no longer accepting contributions.");
        }

        Consumer consumer = consumerRepository.findById(consumerId)
            .orElseThrow(() -> new IllegalArgumentException("Consumer not found: " + consumerId));

        // OBSERVER PATTERN: Delegates to Subject — MOQ check and notification inside ✅
        return groupOrderSubject.contribute(order, consumer, quantity);
    }

    /**
     * List all open group orders for a given batch (consumer discovery).
     */
    @Transactional(readOnly = true)
    public List<GroupOrder> getOpenOrdersForBatch(Long batchId) {
        return groupOrderRepository.findByHarvestBatch_BatchIdAndStatus(
            batchId, GroupOrder.OrderStatus.OPEN);
    }

    /**
     * Retrieve all group orders a specific consumer participates in.
     */
    @Transactional(readOnly = true)
    public List<GroupOrder> getOrdersByConsumer(Long consumerId) {
        return groupOrderRepository.findOrdersByConsumer(consumerId);
    }

    // -------------------------------------------------------
    // BILL SPLITTING & PAYMENT (Minor Feature — Module 2)
    // -------------------------------------------------------

    /**
     * SRP: Payment splitting logic lives ONLY here, not in GroupOrder entity.
     *
     * After MOQ is confirmed, splits the total bill equally among all
     * participants and creates one PaymentTransaction per consumer.
     *
     * @param orderId       the confirmed group order
     * @param pricePerKg    the negotiated/dynamic price per kg
     * @param paymentMethod payment method string (e.g. "UPI")
     * @return              list of created PaymentTransaction records
     */
    public List<PaymentTransaction> splitAndPay(Long orderId,
                                                BigDecimal pricePerKg,
                                                String paymentMethod) {
        GroupOrder order = groupOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("GroupOrder not found: " + orderId));

        if (order.getStatus() != GroupOrder.OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order must be CONFIRMED before payment. Current: " + order.getStatus());
        }

        List<Consumer> participants = order.getParticipants();
        if (participants.isEmpty()) {
            throw new IllegalStateException("No participants in order #" + orderId);
        }

        // Total bill = total pooled quantity × price per kg
        BigDecimal totalBill = pricePerKg.multiply(BigDecimal.valueOf(order.getPoolTotalQuantity()));

        // Each consumer's share = totalBill / number of participants
        // NOTE: In a real system each consumer pays proportionally to their contribution
        BigDecimal perPersonShare = totalBill
            .divide(BigDecimal.valueOf(participants.size()), 2, RoundingMode.HALF_UP);

        // SRP: Creating PaymentTransaction is the concern of THIS method alone,
        //      not of GroupOrder or HarvestBatch.
        List<PaymentTransaction> transactions = participants.stream().map(consumer -> {
            PaymentTransaction tx = new PaymentTransaction();
            tx.setGroupOrder(order);
            tx.setConsumer(consumer);
            tx.setTotalAmount(perPersonShare);
            tx.setPaymentMethod(paymentMethod);
            tx.setPaymentStatus(PaymentTransaction.PaymentStatus.SUCCESS);
            return paymentTransactionRepository.save(tx);
        }).toList();

        return transactions;
    }
}
