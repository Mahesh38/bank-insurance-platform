package com.bank.insurance.onesb.api;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CR-002 — the OpenAPI specification is a local and dev testing aid, never a served surface.
 *
 * <p>The service is reachable inside the VPC only and the specification exists so a developer
 * can exercise the API without hand-building a request collection. It is deliberately not
 * published anywhere, and the spec endpoints must not answer on UAT or production.
 *
 * <p>This is easy to regress and hard to notice: springdoc is enabled by default, so the failure
 * mode is a Swagger UI quietly answering on a deployed host — which is exactly the state this
 * repository was in before CR-002. `render.yaml` publishes port 8080, so on that deployment the
 * browser was publicly reachable.
 *
 * <p>The disabling is configuration, so only a test that boots the profile can prove it. Reading
 * the YAML would only prove the YAML says the right thing.
 */
@Tag("integration")
@Tag("SEC")
class OpenApiNotExposedTest {

    private static final String API_DOCS = "/v3/api-docs";
    private static final String SWAGGER_UI = "/swagger-ui/index.html";

    /**
     * A 404 (no handler) or a 401/403 both count as "not exposed"; a 200 does not. Asserting on
     * "not 200" rather than on an exact status keeps the test about the property that matters.
     */
    private static void assertNotServed(MockMvc mockMvc, String path, String profile)
            throws Exception {
        int status = mockMvc.perform(MockMvcRequestBuilders.get(path))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .as("%s must not be served under the %s profile (CR-002) — got HTTP %d",
                        path, profile, status)
                .isNotEqualTo(200);
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles({"test", "uat"})
    @TestPropertySource(properties = {
            // Prove the pin, not the default: an operator setting this must not re-enable the
            // spec on UAT.
            "SPRINGDOC_ENABLED=true",
            // The uat profile resolves credentials from real OS environment variables; swap in
            // the stub provider so this test is about exposure and nothing else.
            "insurance.secrets.source=PROPERTIES",
            "onesb.api-key=test-api-key",
            "onesb.api-secret=test-api-secret",
            "onesb.distributor-id=TEST_DIST",
            "onesb.client.base-url=http://localhost:9",
            "bank.persistence.base-url=http://localhost:9",
    })
    class UatProfile {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void apiDocsAreNotServed() throws Exception {
            assertNotServed(mockMvc, API_DOCS, "uat");
        }

        @Test
        void swaggerUiIsNotServed() throws Exception {
            assertNotServed(mockMvc, SWAGGER_UI, "uat");
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles({"test", "prod"})
    @TestPropertySource(properties = {
            "SPRINGDOC_ENABLED=true",
            // The prod profile pins AWS_SECRETS_MANAGER, which is a deliberate fail-fast stub
            // (TD-006). Swap in the stub provider so this test is about exposure and nothing else.
            "insurance.secrets.source=PROPERTIES",
            "onesb.api-key=test-api-key",
            "onesb.api-secret=test-api-secret",
            "onesb.distributor-id=TEST_DIST",
            "onesb.client.base-url=http://localhost:9",
            "bank.persistence.base-url=http://localhost:9",
    })
    class ProdProfile {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void apiDocsAreNotServed() throws Exception {
            assertNotServed(mockMvc, API_DOCS, "prod");
        }

        @Test
        void swaggerUiIsNotServed() throws Exception {
            assertNotServed(mockMvc, SWAGGER_UI, "prod");
        }
    }

    /**
     * With no profile-specific override the default must be off, so a new environment is safe
     * before anyone configures it.
     */
    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("default-springdoc")
    @TestPropertySource(properties = {
            "insurance.secrets.source=PROPERTIES",
            "onesb.api-key=test-api-key",
            "onesb.api-secret=test-api-secret",
            "onesb.distributor-id=TEST_DIST",
            "onesb.client.base-url=http://localhost:9",
            "bank.persistence.base-url=http://localhost:9",
    })
    class NoProfileOverride {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void apiDocsAreOffByDefault() throws Exception {
            assertNotServed(mockMvc, API_DOCS, "no override");
        }
    }
}
