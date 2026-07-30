package com.bank.insurance.onesb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifies the Spring application context loads with H2 in-memory DB
 * and test credentials. Uses the 'test' profile to skip secrets validation.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "onesb.api-key=test-api-key",
        "onesb.api-secret=test-api-secret",
        "onesb.distributor-id=TEST_DIST",
        "spring.datasource.url=jdbc:h2:mem:onesb_smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "insurance.secrets.source=PROPERTIES"
})
class ApplicationContextTest {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }
}
