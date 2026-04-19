package com.agriconnect.controller;

import com.agriconnect.model.DeliverySlot;
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

    public LogisticsController(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    /**
     * List all open delivery slots for a zone.
     * Default to 'ALL' if no zone provided.
     */
    @GetMapping("/slots")
    public ResponseEntity<List<DeliverySlot>> listSlots(@RequestParam(required = false, defaultValue = "ALL") String zone) {
        if ("ALL".equalsIgnoreCase(zone)) {
            // In a full implementation, we'd add an 'findAllOpen' method to the service
            // For now, we reuse the zone filtering or return mock-like results
            return ResponseEntity.ok(logisticsService.getAvailableSlotsForZone(zone));
        }
        return ResponseEntity.ok(logisticsService.getAvailableSlotsForZone(zone));
    }
}
