package com.agriconnect.pattern.factory;

import com.agriconnect.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * ============================================================
 * PART 2: DESIGN PATTERN - Factory Method: Concrete Creator
 * ============================================================
 *
 * AgriBoxFactory — the Concrete Creator that implements the
 * factory method for producing different SubscriptionBox types.
 *
 * FACTORY PATTERN FLOW:
 *   SubscriptionService → calls createSubscription("VEGGIE", ...) 
 *       → AgriBoxFactory decides → returns new VeggieBox()
 *       SubscriptionService never uses `new VeggieBox()` directly.
 *
 * DESIGN PRINCIPLE: DIP (Dependency Inversion)
 *   SubscriptionService is injected with SubscriptionFactory
 *   (interface / abstraction), not AgriBoxFactory (concrete class).
 *   This means the concrete factory can be swapped out (e.g. for
 *   testing with a MockSubscriptionFactory) without touching
 *   SubscriptionService. High-level module (service) does not
 *   depend on low-level module (factory implementation). ✅
 */
@Component
public class AgriBoxFactory implements SubscriptionFactory {

    /** Price map for box types — could be loaded from database in production. */
    private static final Map<String, Double> PRICE_MAP = Map.of(
        "VEGGIE", 299.0,
        "FRUIT",  349.0
    );

    /**
     * Factory Method implementation — decides which concrete
     * SubscriptionBox subclass to instantiate based on boxType.
     *
     * OCP: Adding "GRAIN" requires only a new GrainBox class
     *      and one more case below. Zero other changes.
     */
    @Override
    public SubscriptionBox createSubscription(String boxType,
                                              Consumer consumer,
                                              String frequency,
                                              String preference) {
        SubscriptionBox box;

        switch (boxType.toUpperCase()) {

            case "VEGGIE" -> {
                VeggieBox veggie = new VeggieBox();
                veggie.setVeggiePreference(preference != null ? preference : "SEASONAL");
                box = veggie;
            }

            case "FRUIT" -> {
                FruitBox fruit = new FruitBox();
                fruit.setFruitPreference(preference != null ? preference : "MIXED");
                box = fruit;
            }

            default -> throw new IllegalArgumentException(
                "Unknown box type: '" + boxType + "'. Supported: VEGGIE, FRUIT");
        }

        // Set shared fields from SubscriptionBox base class
        box.setConsumer(consumer);
        box.setFrequency(frequency.toUpperCase());
        box.setNextDeliveryDate(LocalDate.now().plusDays(3)); // First delivery in 3 days
        box.setActive(true);
        box.setPricePerCycle(PRICE_MAP.getOrDefault(boxType.toUpperCase(), 299.0));

        return box;
    }
}
