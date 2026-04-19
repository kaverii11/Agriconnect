package com.agriconnect.pattern.factory;

import com.agriconnect.model.Consumer;
import com.agriconnect.model.SubscriptionBox;

/**
 * ============================================================
 * PART 2: DESIGN PATTERN - Factory Method (Module 4)
 * ============================================================
 *
 * PATTERN: Factory Method (Creational)
 *   Defines an interface for CREATING objects, but lets
 *   subclasses decide which class to instantiate.
 *   Factory Method defers instantiation to subclasses.
 *
 * WHY FACTORY HERE?
 *   Different consumers want different subscription types
 *   (VeggieBox, FruitBox, MixedBox). The calling code (service layer)
 *   should NOT use `new VeggieBox()` directly — that would tightly
 *   couple callers to concrete classes, violating DIP.
 *
 * DESIGN PRINCIPLE: OCP
 *   Adding a new box type (e.g., OrganicGrainBox):
 *   1. Create: class OrganicGrainBox extends SubscriptionBox
 *   2. Add "GRAIN" case to SubscriptionFactory.createSubscription()
 *   SubscriptionService never changes. ✅
 *
 * DESIGN PRINCIPLE: DIP
 *   SubscriptionService depends on THIS interface (abstraction)
 *   not on VeggieBox or FruitBox (concretions).
 */
public interface SubscriptionFactory {

    /**
     * Factory Method — creates the appropriate SubscriptionBox
     * based on the box type key.
     *
     * @param boxType   "VEGGIE", "FRUIT", or other mapped type
     * @param consumer  the subscribing Consumer
     * @param frequency subscription cadence ("WEEKLY", "MONTHLY" etc.)
     * @param preference consumer's item preference (e.g. "TROPICAL")
     * @return a fully initialised SubscriptionBox ready to persist
     */
    SubscriptionBox createSubscription(String boxType,
                                       Consumer consumer,
                                       String frequency,
                                       String preference);
}
