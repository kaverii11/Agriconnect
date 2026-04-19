package com.agriconnect.service;

import com.agriconnect.model.Consumer;
import com.agriconnect.model.DeliverySlot;
import com.agriconnect.model.ReturnRequest;
import com.agriconnect.pattern.facade.ILogisticsAdminService;
import com.agriconnect.repository.ConsumerRepository;
import com.agriconnect.repository.DeliverySlotRepository;
import com.agriconnect.repository.ReturnRequestRepository;
import com.agriconnect.repository.PaymentTransactionRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================
 * SERVICE LAYER - Module 3: Logistics & Slot Management
 * ============================================================
 *
 * MVC ROLE: Business logic layer. Called by LogisticsController.
 *
 * DESIGN PRINCIPLE: ISP (Interface Segregation Principle)
 *   LogisticsService implements BOTH interfaces:
 *     - ILogisticsAdminService (extends IConsumerDeliveryService)
 *   When injected into CheckoutFacade → typed as IConsumerDeliveryService
 *     → consumer can only call bookDeliverySlot(), cancelDeliveryBooking(),
 *       getAvailableSlotsForZone().
 *   When injected into LogisticsController → typed as ILogisticsAdminService
 *     → coordinator can additionally call createSlot(), closeSlot().
 *
 * This is ISP in action: same implementation class, different interface
 * "views" depending on who is consuming it.
 *
 * DESIGN PATTERN: CheckoutFacade (in facade package) calls THIS service
 *   via the narrow IConsumerDeliveryService reference — demonstrating
 *   how Facade + ISP work together in Module 3.
 *
 * @Primary ensures Spring picks THIS bean when auto-wiring
 *   IConsumerDeliveryService (e.g. in CheckoutFacade).
 */
@Service
@Primary
@Transactional
public class LogisticsService implements ILogisticsAdminService {

    private final DeliverySlotRepository     deliverySlotRepository;
    private final ConsumerRepository         consumerRepository;
    private final ReturnRequestRepository    returnRequestRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public LogisticsService(DeliverySlotRepository deliverySlotRepository,
                            ConsumerRepository consumerRepository,
                            ReturnRequestRepository returnRequestRepository,
                            PaymentTransactionRepository paymentTransactionRepository) {
        this.deliverySlotRepository       = deliverySlotRepository;
        this.consumerRepository           = consumerRepository;
        this.returnRequestRepository      = returnRequestRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    // -------------------------------------------------------
    // IConsumerDeliveryService methods (narrow — consumer-facing)
    // -------------------------------------------------------

    /**
     * ISP: Consumer-accessible booking. Narrow interface only exposes this.
     * Called via CheckoutFacade using the IConsumerDeliveryService reference.
     */
    @Override
    public DeliverySlot bookDeliverySlot(Long slotId, Long consumerId) {
        DeliverySlot slot = deliverySlotRepository.findById(slotId)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        // Entity encapsulation: book() validates capacity internally
        slot.book();
        return deliverySlotRepository.save(slot);
    }

    @Override
    public DeliverySlot cancelDeliveryBooking(Long slotId, Long consumerId) {
        DeliverySlot slot = deliverySlotRepository.findById(slotId)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));
        slot.cancelBooking();
        return deliverySlotRepository.save(slot);
    }

    @Override
    public List<DeliverySlot> getAvailableSlotsForZone(String zone) {
        return deliverySlotRepository.findByZoneAndSlotStatus(zone, DeliverySlot.SlotStatus.OPEN);
    }

    // -------------------------------------------------------
    // ILogisticsAdminService methods (wide — coordinator-only)
    // ISP: These methods are NOT available to consumers because
    // the CheckoutFacade only holds an IConsumerDeliveryService ref.
    // -------------------------------------------------------

    /**
     * COORDINATOR ONLY: Creates a new delivery slot.
     * ISP: Consumers never access this via IConsumerDeliveryService.
     */
    @Override
    public DeliverySlot createSlot(LocalDateTime slotTime, int maxCapacity,
                                   String zone, Long coordinatorId) {
        DeliverySlot slot = new DeliverySlot();
        slot.setSlotTime(slotTime);
        slot.setMaxCapacity(maxCapacity);
        slot.setZone(zone);
        slot.setCurrentBookings(0);
        slot.setSlotStatus(DeliverySlot.SlotStatus.OPEN);
        // Note: Coordinator lookup would go here in full implementation
        return deliverySlotRepository.save(slot);
    }

    /** COORDINATOR ONLY: Closes a slot for new bookings. */
    @Override
    public DeliverySlot closeSlot(Long slotId) {
        DeliverySlot slot = deliverySlotRepository.findById(slotId)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));
        slot.setSlotStatus(DeliverySlot.SlotStatus.CLOSED);
        return deliverySlotRepository.save(slot);
    }

    /** COORDINATOR ONLY: Administrative view of all slots. */
    @Override
    public List<DeliverySlot> getAllSlotsForCoordinator(Long coordinatorId) {
        return deliverySlotRepository.findByCoordinator_UserId(coordinatorId);
    }

    // -------------------------------------------------------
    // FRESHNESS GUARANTEE / RETURNS (Minor Feature — Module 3)
    // -------------------------------------------------------

    /**
     * Consumer submits a return request with proof image.
     *
     * @param transactionId  the payment transaction to return against
     * @param consumerId     the requesting consumer
     * @param reason         reason text for the return
     * @param proofImageUrl  URL of uploaded proof image
     */
    public ReturnRequest submitReturnRequest(Long transactionId,
                                            Long consumerId,
                                            String reason,
                                            String proofImageUrl) {
        Consumer consumer = consumerRepository.findById(consumerId)
            .orElseThrow(() -> new IllegalArgumentException("Consumer not found: " + consumerId));

        if (transactionId == null) throw new IllegalArgumentException("Transaction ID is null");
        var transaction = paymentTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        ReturnRequest request = new ReturnRequest();
        request.setConsumer(consumer);
        request.setTransaction(transaction);
        request.setReason(reason);
        request.setProofImage(proofImageUrl);
        request.setReturnStatus(ReturnRequest.ReturnStatus.PENDING);

        return returnRequestRepository.save(request);
    }

    /** Admin approves a return request (triggers refund in production). */
    public ReturnRequest approveReturn(Long requestId) {
        ReturnRequest request = returnRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("ReturnRequest not found: " + requestId));
        request.setReturnStatus(ReturnRequest.ReturnStatus.APPROVED);
        // In production: trigger payment gateway refund here
        return returnRequestRepository.save(request);
    }

    /** Get all pending returns (for admin dashboard). */
    @Transactional(readOnly = true)
    public List<ReturnRequest> getPendingReturns() {
        return returnRequestRepository.findByReturnStatus(ReturnRequest.ReturnStatus.PENDING);
    }
}
