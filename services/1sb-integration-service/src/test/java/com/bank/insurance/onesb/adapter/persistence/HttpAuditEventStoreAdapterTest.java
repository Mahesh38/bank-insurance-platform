package com.bank.insurance.onesb.adapter.persistence;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditOutcomes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * RISK-012 — the durable audit sink.
 *
 * <p>Two properties matter here and they pull against each other: the event must reach
 * {@code audit_event}, and failing to send it must never fail the customer's transaction. A
 * test that only proved the happy path would leave the second property to hope.
 */
@Tag("unit")
@Tag("COMP-002")
class HttpAuditEventStoreAdapterTest {

    private static final String PATH = "/internal/v1/audit-events";

    private static final WireMockServer PERSISTENCE =
            new WireMockServer(wireMockConfig().dynamicPort());

    static {
        PERSISTENCE.start();
    }

    @AfterAll
    static void stop() {
        PERSISTENCE.stop();
    }

    private HttpAuditEventStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        PERSISTENCE.resetAll();
        adapter = new HttpAuditEventStoreAdapter(restClient(PERSISTENCE.baseUrl()), new ObjectMapper());
    }

    private static RestClient restClient(String baseUrl) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    private static void stubAccepted() {
        PERSISTENCE.stubFor(post(urlEqualTo(PATH))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"eventId\":\"evt-1\"}")));
    }

    @Test
    void appendsTheEventWithEveryFieldTheSchemaRequires() {
        stubAccepted();

        adapter.publish(AuditEvent.builder()
                .eventId("evt-1")
                .timestamp(Instant.parse("2026-08-14T10:00:00Z"))
                .actorId("rm-4471")
                .actorType("USER")
                .action(AuditActions.PROPOSAL_SUBMITTED)
                .resourceType("PROPOSAL_JOB")
                .resourceId("job-1")
                .outcome(AuditOutcomes.SUCCESS)
                .lob("TERM")
                .journeyId("journey-1")
                .distributorId("BCIBL")
                .agentId("109337")
                .traceId("trace-1")
                .build());

        PERSISTENCE.verify(exactly(1), postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(matchingJsonPath("$.eventId", equalTo("evt-1")))
                .withRequestBody(matchingJsonPath("$.action", equalTo("PROPOSAL_SUBMITTED")))
                .withRequestBody(matchingJsonPath("$.actorId", equalTo("rm-4471")))
                .withRequestBody(matchingJsonPath("$.actorType", equalTo("USER")))
                .withRequestBody(matchingJsonPath("$.resourceType", equalTo("PROPOSAL_JOB")))
                .withRequestBody(matchingJsonPath("$.resourceId", equalTo("job-1")))
                .withRequestBody(matchingJsonPath("$.outcome", equalTo("SUCCESS")))
                .withRequestBody(matchingJsonPath("$.journeyId", equalTo("journey-1")))
                .withRequestBody(matchingJsonPath("$.traceId", equalTo("trace-1"))));
    }

    /**
     * The schema says metadata must not carry PII. This is the mechanism behind that rule — a
     * caller who puts a mobile number in metadata produces a masked row, not an incident.
     */
    @Test
    void masksPiiThatACallerPutInMetadata() {
        stubAccepted();

        adapter.publish(minimal()
                .metadata("mobile", "9876543210")
                .metadata("pan", "ABCDE1234F")
                .metadata("note", "contact asha.rao@example.com")
                .build());

        String body = PERSISTENCE.findAll(postRequestedFor(urlEqualTo(PATH)))
                .get(0).getBodyAsString();

        assertThat(body)
                .doesNotContain("9876543210")
                .doesNotContain("ABCDE1234F")
                .doesNotContain("asha.rao@example.com");
        assertThat(body).contains("3210");
    }

    @Test
    void omitsMetadataEntirelyWhenThereIsNone() {
        stubAccepted();

        adapter.publish(minimal().build());

        assertThat(PERSISTENCE.findAll(postRequestedFor(urlEqualTo(PATH))).get(0).getBodyAsString())
                .doesNotContain("\"metadata\":{");
    }

    /** Optional-in-our-model fields are required by the persistence schema — defaulted, not null. */
    @Test
    void substitutesDefaultsForFieldsThePersistenceSchemaRequires() {
        stubAccepted();

        adapter.publish(AuditEvent.builder()
                .actorId("system")
                .action(AuditActions.ONESB_OUTBOUND_CALL)
                .outcome(AuditOutcomes.SUCCESS)
                .build());

        PERSISTENCE.verify(exactly(1), postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(matchingJsonPath("$.actorType", equalTo("SYSTEM")))
                .withRequestBody(matchingJsonPath("$.resourceType", equalTo("UNKNOWN")))
                .withRequestBody(matchingJsonPath("$.resourceId", equalTo("UNKNOWN"))));
    }

    // --- the property that protects the customer ------------------------------------------

    @Test
    void persistenceReturning500DoesNotThrow() {
        PERSISTENCE.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(500)));

        assertThatCode(() -> adapter.publish(minimal().build())).doesNotThrowAnyException();
    }

    @Test
    void persistenceBeingUnreachableDoesNotThrow() {
        HttpAuditEventStoreAdapter unreachable = new HttpAuditEventStoreAdapter(
                restClient("http://localhost:9"), new ObjectMapper());

        assertThatCode(() -> unreachable.publish(minimal().build())).doesNotThrowAnyException();
    }

    @Test
    void nullEventIsIgnoredRatherThanThrowing() {
        assertThatCode(() -> adapter.publish(null)).doesNotThrowAnyException();
        PERSISTENCE.verify(0, postRequestedFor(urlEqualTo(PATH)));
    }

    /**
     * If metadata cannot be serialised we send an empty object rather than falling back to
     * {@code toString()} — an unmasked fallback would defeat the masking above.
     */
    @Test
    void unserialisableMetadataIsDroppedRatherThanSentUnmasked() {
        stubAccepted();
        ObjectMapper failing = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
                throw new com.fasterxml.jackson.core.JsonProcessingException("boom") {};
            }
        };
        HttpAuditEventStoreAdapter adapterWithFailingMapper =
                new HttpAuditEventStoreAdapter(restClient(PERSISTENCE.baseUrl()), failing);

        adapterWithFailingMapper.publish(minimal().metadata("mobile", "9876543210").build());

        assertThat(PERSISTENCE.findAll(postRequestedFor(urlEqualTo(PATH))).get(0).getBodyAsString())
                .doesNotContain("9876543210");
    }

    private static AuditEvent.AuditEventBuilder minimal() {
        return AuditEvent.builder()
                .actorId("rm-1")
                .actorType("USER")
                .action(AuditActions.QUOTE_JOB_CREATED)
                .resourceType("QUOTE_JOB")
                .resourceId("job-1")
                .outcome(AuditOutcomes.SUCCESS);
    }

    /** Guards the assumption that a masked mobile keeps its last four digits. */
    @Test
    void maskedMobileStillSupportsALookup() {
        stubAccepted();

        adapter.publish(minimal().metadata("mobile", "9876543210").build());

        List<com.github.tomakehurst.wiremock.verification.LoggedRequest> requests =
                PERSISTENCE.findAll(postRequestedFor(urlEqualTo(PATH)));
        assertThat(requests.get(0).getBodyAsString()).contains("3210");
    }
}
