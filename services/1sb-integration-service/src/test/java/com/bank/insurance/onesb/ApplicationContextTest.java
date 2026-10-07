package com.bank.insurance.onesb;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.common.apigee.ApigeeAccessTokenHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
        "onesb.client.base-url=http://localhost:9",
        "insurance.secrets.source=PROPERTIES",
        "bank.persistence.base-url=http://localhost:8081"
})
class ApplicationContextTest {

    @Autowired ApigeeAccessTokenHolder apigeeAccessTokenHolder;

    @Test
    void contextLoads() {
        assertThat(apigeeAccessTokenHolder.currentAccessToken()).isEqualTo("stub-apigee-token");
    }
}
