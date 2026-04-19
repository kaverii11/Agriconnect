package com.agriconnect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * OOP CONCEPT: INHERITANCE
 * Administrator IS-A User with elevated system privileges.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "administrators")
@DiscriminatorValue("ADMIN")
@PrimaryKeyJoinColumn(name = "user_id")
public class Administrator extends User {

    @Column(name = "department")
    private String department;

    @Column(name = "access_level")
    private Integer accessLevel = 1;

    @Override
    public String getRole() {
        return "ADMINISTRATOR";
    }
}
