package com.agriconnect.controller;

import com.agriconnect.model.HarvestBatch;
import com.agriconnect.service.FarmerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * PART 3: CONTROLLER LAYER - Farmer Inventory Management
 * ============================================================
 *
 * MVC ROLE: Controller
 * - Acts as an intermediary between the View (Client/Postman)
 *   and the Model (FarmerService).
 * - Handles HTTP request parsing, input validation, and
 *   determines the appropriate response.
 *
 * DESIGN PRINCIPLE: Separation of Concerns
 * - This controller does NOT contain business logic.
 * - It delegates pricing calculations or analytics to the
 *   FarmerService, ensuring the system remains maintainable.
 */
@RestController
@RequestMapping("/api/farmer")
public class FarmerController {

    private final FarmerService farmerService;

    public FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    /**
     * MODULE 1: CRUD for Harvest Batches
     * REST endpoint to register new produce for the platform.
     */
    @PostMapping("/{farmerId}/batches")
    public ResponseEntity<HarvestBatch> registerHarvest(@PathVariable Long farmerId,
                                                       @RequestBody HarvestBatch batch) {
        return ResponseEntity.ok(farmerService.createHarvestBatch(farmerId, batch));
    }

    @GetMapping("/{farmerId}/batches")
    public ResponseEntity<List<HarvestBatch>> listMyBatches(@PathVariable Long farmerId) {
        return ResponseEntity.ok(farmerService.getBatchesByFarmer(farmerId));
    }

    /**
     * MODULE 1 (Minor): Dynamic Pricing Tool
     * Demonstrates the Strategy Pattern via the Service layer.
     * The farmer can 'preview' what a consumer's price would be based on quantity.
     */
    @GetMapping("/batches/{batchId}/price-preview")
    public ResponseEntity<BigDecimal> getPricePreview(@PathVariable Long batchId,
                                                      @RequestParam double quantity) {
        BigDecimal price = farmerService.calculatePrice(batchId, quantity);
        return ResponseEntity.ok(price);
    }

    /**
     * MODULE 4: Farmer Analytics Dashboard
     * Provides aggregated data on produce sales.
     */
    @GetMapping("/{farmerId}/dashboard/analytics")
    public ResponseEntity<Map<String, Double>> getFarmerDashboard(@PathVariable Long farmerId) {
        return ResponseEntity.ok(farmerService.getSalesSummary(farmerId));
    }
}
