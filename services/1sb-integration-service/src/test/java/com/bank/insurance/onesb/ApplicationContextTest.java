package com.bank.insurance.onesb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifies the Spring application context loads with test credentials.
 * Uses the 'test' profile to skip secrets validation. Persistence HTTP calls are
 * lazy (RestClient bean only); no WireMock required for context load.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "onesb.api-key=test-api-key",
        "onesb.api-secret=test-api-secret",
        "onesb.distributor-id=TEST_DIST",
        "insurance.secrets.source=PROPERTIES",
        "bank.persistence.base-url=http://localhost:8081"
})
class ApplicationContextTest {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }
}
