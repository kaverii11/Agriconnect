package com.agriconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ================================================================
 * AgriConnect - Farm-to-Table Community Commerce Platform
 * OOAD Mini Project | Java 17 | Spring Boot 3.x
 * ================================================================
 *
 * Architecture: Strict MVC (Model → Service → Controller)
 * Design Patterns Implemented:
 *   1. Strategy   - Module 1: Dynamic Pricing
 *   2. Observer   - Module 2: Group Buying notifications
 *   3. Facade     - Module 3: Checkout simplification
 *   4. Factory    - Module 4: Subscription creation
 */
@SpringBootApplication
public class AgriConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgriConnectApplication.class, args);
    }
}
