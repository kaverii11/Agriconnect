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
 * Consumer IS-A User. Participates in GroupOrders.
 *
 * DESIGN NOTE (ISP — Module 3):
 * Consumer-facing delivery booking uses a narrow
 * IConsumerDeliveryService interface, not the full
 * ILogisticsAdminService, satisfying Interface Segregation.
 */
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "consumers")
@DiscriminatorValue("CONSUMER")
@PrimaryKeyJoinColumn(name = "user_id")
public class Consumer extends User {

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "wallet_balance", nullable = false)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    /**
     * JPA: @ManyToMany — a Consumer can join many GroupOrders,
     * and a GroupOrder can have many Consumers.
     * Managed via join table "consumer_group_orders".
     */
    @JsonIgnore
    @ManyToMany(mappedBy = "participants")
    private List<GroupOrder> groupOrders = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "consumer", cascade = CascadeType.ALL)
    private List<SubscriptionBox> subscriptions = new ArrayList<>();

    @Override
    public String getRole() {
        return "CONSUMER";
    }
}
