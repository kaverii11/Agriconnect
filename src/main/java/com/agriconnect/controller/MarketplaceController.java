package com.agriconnect.controller;

import com.agriconnect.model.HarvestBatch;
import com.agriconnect.model.GroupOrder;
import com.agriconnect.repository.HarvestBatchRepository;
import com.agriconnect.repository.GroupOrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * NEW: CONTROLLER LAYER - Marketplace Discovery
 * ============================================================
 * 
 * Purpose: Provides read-only endpoints for consumers to discover 
 * produce and active buying pools.
 */
@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final HarvestBatchRepository harvestBatchRepository;
    private final GroupOrderRepository groupOrderRepository;

    public MarketplaceController(HarvestBatchRepository harvestBatchRepository,
                                 GroupOrderRepository groupOrderRepository) {
        this.harvestBatchRepository = harvestBatchRepository;
        this.groupOrderRepository = groupOrderRepository;
    }

    /**
     * Returns all available harvest batches (non-expired, with stock).
     */
    @GetMapping("/produce")
    public ResponseEntity<List<HarvestBatch>> listAvailableProduce() {
        List<HarvestBatch> all = harvestBatchRepository.findAll();
        // Filter for availability: not expired and has stock
        List<HarvestBatch> available = all.stream()
                .filter(b -> !b.isExpired() && b.getAvailableQuantity() > 0)
                .collect(Collectors.toList());
        return ResponseEntity.ok(available);
    }

    /**
     * Returns all active group buying pools.
     */
    @GetMapping("/active-pools")
    public ResponseEntity<List<GroupOrder>> listActivePools() {
        return ResponseEntity.ok(groupOrderRepository.findByStatus(GroupOrder.OrderStatus.OPEN));
    }
}
