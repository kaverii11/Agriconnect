package com.agriconnect.controller;

import com.agriconnect.model.GroupOrder;
import com.agriconnect.model.PaymentTransaction;
import com.agriconnect.pattern.facade.CheckoutFacade;
import com.agriconnect.service.GroupBuyingService;
import com.agriconnect.service.LogisticsService;
import com.agriconnect.repository.ConsumerRepository;
import com.agriconnect.model.Consumer;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * ============================================================
 * PART 3: CONTROLLER LAYER - Consumer & Group Buying
 * ============================================================
 *
 * MVC ROLE: Controller
 * - Orchestrates various services to fulfill complex user requests.
 *
 * DESIGN PATTERN: FACADE PATTERN (Module 3 Integration)
 * - Notice how simplified the `fullCheckout` method is.
 * - Instead of the controller knowing about inventory, slots, and payments,
 *   it simply calls the CheckoutFacade. This keeps the controller lean
 *   and the architecture robust.
 *
 * DESIGN PRINCIPLE: Single Responsibility Principle (SRP)
 * - This controller handles only consumer-facing order operations.
 */
@RestController
@RequestMapping("/api/consumer")
public class ConsumerOrderController {

    private final GroupBuyingService groupBuyingService;
    private final CheckoutFacade checkoutFacade;
    private final LogisticsService logisticsService;
    private final ConsumerRepository consumerRepository;

    public ConsumerOrderController(GroupBuyingService groupBuyingService,
                                   CheckoutFacade checkoutFacade,
                                   LogisticsService logisticsService,
                                   ConsumerRepository consumerRepository) {
        this.groupBuyingService = groupBuyingService;
        this.checkoutFacade = checkoutFacade;
        this.logisticsService = logisticsService;
        this.consumerRepository = consumerRepository;
    }

    /**
     * MODULE 2: Group Buying Engine
     * Consumers join a pool to meet MOQ.
     */
    @PostMapping("/{consumerId}/orders/{orderId}/join")
    public ResponseEntity<GroupOrder> joinGroupBuying(@PathVariable Long consumerId,
                                                     @PathVariable Long orderId,
                                                     @RequestParam double quantity) {
        GroupOrder updatedOrder = groupBuyingService.contribute(orderId, consumerId, quantity);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * Start a NEW community pool for a specific batch.
     */
    @PostMapping("/orders/start")
    public ResponseEntity<GroupOrder> startNewPool(@RequestParam Long batchId,
                                                   @RequestParam double moq) {
        GroupOrder newOrder = groupBuyingService.openGroupOrder(batchId, moq);
        return ResponseEntity.ok(newOrder);
    }

    /**
     * List all orders a consumer has joined.
     */
    @GetMapping("/{consumerId}/my-orders")
    public ResponseEntity<java.util.List<GroupOrder>> listMyOrders(@PathVariable Long consumerId) {
        return ResponseEntity.ok(groupBuyingService.getOrdersByConsumer(consumerId));
    }

    /**
     * MODULE 3: Integrated Checkout via Facade
     * Demonstrates the Structural Facade Pattern.
     * The controller is shielded from the complex multi-step process.
     */
    @PostMapping("/checkout")
    public ResponseEntity<PaymentTransaction> fullCheckout(@RequestBody CheckoutRequest request) {
        // Load the real Consumer entity from the database
        Consumer consumer = consumerRepository.findById(request.getConsumerId())
                .orElseThrow(() -> new IllegalArgumentException("Consumer not found: " + request.getConsumerId()));

        // Call the Facade with the real consumer object
        PaymentTransaction tx = checkoutFacade.processCheckout(
                request.getOrderId(),
                request.getSlotId(),
                consumer,
                request.getAmount(),
                request.getPaymentMethod()
        );
        return ResponseEntity.ok(tx);
    }

    /**
     * MODULE 3 (Minor): Quality Returns
     * Consumer reports issues with produce freshness.
     */
    @PostMapping("/{consumerId}/returns")
    public ResponseEntity<String> requestReturn(@PathVariable Long consumerId,
                                               @RequestParam Long transactionId,
                                               @RequestParam String reason) {
        logisticsService.submitReturnRequest(transactionId, consumerId, reason, "uploaded_proof_url");
        return ResponseEntity.ok("Return request submitted for evaluation.");
    }

    @Data
    static class CheckoutRequest {
        private Long orderId;
        private Long slotId;
        private Long consumerId;
        private BigDecimal amount;
        private String paymentMethod;
    }
}
