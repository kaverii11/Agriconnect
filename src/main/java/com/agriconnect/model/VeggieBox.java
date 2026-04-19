package com.agriconnect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Factory Pattern Product: VeggieBox
 * Created by SubscriptionFactory.createSubscription("VEGGIE", ...)
 */
@Getter
@Setter
@Entity
@DiscriminatorValue("VEGGIE_BOX")
public class VeggieBox extends SubscriptionBox {

    @Column(name = "veggie_preference")
    private String veggiePreference; // "SEASONAL", "LEAFY", "ROOT"

    @Override
    public String getBoxDescription() {
        return "Fresh Organic Vegetables Box (" + veggiePreference + ") — " + getFrequency();
    }
}
