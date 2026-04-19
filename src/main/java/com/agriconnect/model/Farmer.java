package com.agriconnect.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * OOP CONCEPT: INHERITANCE & POLYMORPHISM
 * Farmer IS-A User. Overrides getRole() — runtime polymorphism.
 * A Farmer owns many HarvestBatches (one-to-many association).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "farmers")
@DiscriminatorValue("FARMER")
@PrimaryKeyJoinColumn(name = "user_id")
public class Farmer extends User {

    @Column(name = "farm_name")
    private String farmName;

    @Column(name = "farm_location")
    private String farmLocation;

    @Column(name = "verified")
    private boolean verified = false;

    /**
     * OOP: Composition — Farmer HAS-A list of HarvestBatches.
     * JPA: Bidirectional @OneToMany. CascadeType.ALL ensures that
     * when a Farmer is persisted, their batches are too.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "farmer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HarvestBatch> harvestBatches = new ArrayList<>();

    @Override
    public String getRole() {
        return "FARMER"; // Polymorphic method — JVM dispatches at runtime
    }
}
