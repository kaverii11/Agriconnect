package com.agriconnect.controller;

import com.agriconnect.model.DeliverySlot;
import com.agriconnect.repository.DeliverySlotRepository;
import com.agriconnect.service.LogisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * NEW: CONTROLLER LAYER - Logistics & Slots
 * ============================================================
 * 
 * Provides endpoints for consumers to find delivery slots.
 */
@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {

    private final LogisticsService logisticsService;
    private final DeliverySlotRepository deliverySlotRepository;

    public LogisticsController(LogisticsService logisticsService,
                               DeliverySlotRepository deliverySlotRepository) {
        this.logisticsService = logisticsService;
        this.deliverySlotRepository = deliverySlotRepository;
    }

    /**
     * List all open delivery slots. If zone=ALL (default), return every open slot.
     */
    @GetMapping("/slots")
    public ResponseEntity<List<DeliverySlot>> listSlots(@RequestParam(required = false, defaultValue = "ALL") String zone) {
        if ("ALL".equalsIgnoreCase(zone)) {
            return ResponseEntity.ok(deliverySlotRepository.findBySlotStatus(DeliverySlot.SlotStatus.OPEN));
        }
        return ResponseEntity.ok(logisticsService.getAvailableSlotsForZone(zone));
    }
}
