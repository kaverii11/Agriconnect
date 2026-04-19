package com.agriconnect.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * OOP CONCEPT: INHERITANCE
 * LogisticsCoordinator IS-A User who manages delivery slots.
 *
 * DESIGN NOTE (ISP — Module 3):
 * This coordinator uses ILogisticsAdminService which includes
 * slot-management capabilities NOT exposed to consumers.
 * This respects the Interface Segregation Principle.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "logistics_coordinators")
@DiscriminatorValue("LOGISTICS")
@PrimaryKeyJoinColumn(name = "user_id")
public class LogisticsCoordinator extends User {

    @Column(name = "zone_covered")
    private String zoneCovered;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @JsonIgnore
    @OneToMany(mappedBy = "coordinator", cascade = CascadeType.ALL)
    private List<DeliverySlot> managedSlots = new ArrayList<>();

    @Override
    public String getRole() {
        return "LOGISTICS_COORDINATOR";
    }
}
