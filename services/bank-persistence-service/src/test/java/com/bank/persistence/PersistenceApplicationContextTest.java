package com.bank.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PersistenceApplicationContextTest {

    @Test
    void contextLoads() {
        // Verifies Flyway + JPA + web context start on H2
    }
}
