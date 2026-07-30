package com.bank.insurance.onesb;

import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * IT-I template (QA-003): {@code @SpringBootTest} against WireMock 1SB + WireMock persistence.
 * <p>
 * Does <strong>not</strong> call the real 1SB sandbox or real bank-persistence (TESTING-RULES R4).
 * Secrets are satisfied by {@code application-test.yml} ({@code insurance.secrets.source=PROPERTIES}
 * + stub {@code onesb.api-*}), so {@link com.bank.insurance.onesb.config.SecretsStartupValidator}
 * skips validation under the {@code test} profile.
 * <p>
 * <strong>How to run:</strong>
 * <pre>{@code
 * ./gradlew :services:1sb-integration-service:test --tests '*IT'
 * }</pre>
 * Or the full module suite:
 * <pre>{@code
 * ./gradlew :services:1sb-integration-service:test
 * }</pre>
 * <p>
 * Base URLs are bound via {@link DynamicPropertySource} to the two dynamic-port WireMocks
 * started before the Spring context ({@code onesb.client.base-url}, {@code bank.persistence.base-url}).
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class OneSbConnectivityIT {

    private static final WireMockServer ONESB = new WireMockServer(wireMockConfig().dynamicPort());
    private static final WireMockServer PERSISTENCE = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        ONESB.start();
        PERSISTENCE.start();
    }

    @AfterAll
    static void stopWireMocks() {
        ONESB.stop();
        PERSISTENCE.stop();
    }

    @DynamicPropertySource
    static void bindWireMockBaseUrls(DynamicPropertyRegistry registry) {
        registry.add("onesb.client.base-url", ONESB::baseUrl);
        registry.add("bank.persistence.base-url", PERSISTENCE::baseUrl);
    }

    @Autowired
    private OneSbHttpClient oneSbHttpClient;

    @Autowired
    private JobStorePort jobStorePort;

    @BeforeEach
    void resetStubs() {
        ONESB.resetAll();
        PERSISTENCE.resetAll();
    }

    @Test
    void onesbGetProbe_returns200_andCreateJob_returns201Json() {
        ONESB.stubFor(get(urlEqualTo("/v1/probe"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\",\"reqId\":\"it-probe-1\"}")));

        String jobId = "job-it-" + UUID.randomUUID();
        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/jobs"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "jobId": "%s",
                                  "jobType": "QUOTE",
                                  "lob": "TERM",
                                  "status": "PENDING",
                                  "journeyId": "journey-it-1",
                                  "idempotencyKey": "idem-it-1",
                                  "createdAt": "2026-07-30T12:00:00Z",
                                  "updatedAt": "2026-07-30T12:00:00Z",
                                  "version": 0,
                                  "createdByActor": "OneSbConnectivityIT"
                                }
                                """.formatted(jobId))));

        @SuppressWarnings("unchecked")
        Map<String, Object> probe = oneSbHttpClient.get("/v1/probe", Map.class);
        assertThat(probe).containsEntry("status", "ok").containsEntry("reqId", "it-probe-1");

        String created = jobStorePort.createJob(
                "TERM", "QUOTE", "journey-it-1", "idem-it-1", "OneSbConnectivityIT");
        assertThat(created).isEqualTo(jobId);

        ONESB.verify(exactly(1), getRequestedFor(urlEqualTo("/v1/probe"))
                .withHeader("Authorization", containing("Basic ")));
        PERSISTENCE.verify(exactly(1), postRequestedFor(urlEqualTo("/internal/v1/jobs")));
    }
}
