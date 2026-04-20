package com.agriconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * ============================================================
 * PART 1: MODEL LAYER - Abstract Base Class
 * ============================================================
 *
 * OOP CONCEPT: ABSTRACTION & INHERITANCE
 * -  User is declared abstract — it cannot be instantiated
 *    directly. It defines the common contract for all actor
 *    subtypes in the system.
 * -  Concrete subclasses (Farmer, Consumer, etc.) inherit
 *    common fields (userId, name, email, password) via
 *    Java single-inheritance, reducing code duplication.
 *
 * JPA STRATEGY: JOINED (table-per-subclass)
 * -  Each subclass gets its own table, joined by userId.
 *    This preserves normalisation while mapping inheritance.
 *
 * DESIGN PRINCIPLE: Liskov Substitution Principle (LSP)
 * -  Any reference typed as User can be substituted with a
 *    Farmer, Consumer, Administrator, or LogisticsCoordinator
 *    without breaking correctness.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password; // In production: store BCrypt hash

    /**
     * Abstract method — every actor must define its role.
     * Forces subclasses to provide a meaningful identity.
     */
    public abstract String getRole();
}
