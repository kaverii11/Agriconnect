package com.agriconnect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Factory Pattern Product: FruitBox
 * Created by SubscriptionFactory.createSubscription("FRUIT", ...)
 */
@Getter
@Setter
@Entity
@DiscriminatorValue("FRUIT_BOX")
public class FruitBox extends SubscriptionBox {

    @Column(name = "fruit_preference")
    private String fruitPreference; // "TROPICAL", "CITRUS", "MIXED"

    @Override
    public String getBoxDescription() {
        return "Fresh Seasonal Fruits Box (" + fruitPreference + ") — " + getFrequency();
    }
}
