package com.bank.insurance.onesb.adapter.onesb.client;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.config.OneSbClientProperties;
import com.bank.insurance.onesb.adapter.onesb.error.OneSbErrorNormaliser;
import com.bank.insurance.onesb.observability.PiiMasker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class OneSbHttpClientTest {

    private static final String API_KEY = "test-api-key";
    private static final String API_SECRET = "test-api-secret";
    private static final String PAN = "ABCDE1234F";

    private OneSbHttpClient client;
    private OneSbClientProperties properties;
    private RecordingPublisher publisher;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        properties = new OneSbClientProperties(wm.getHttpBaseUrl(), 3000L, 30000L);
        SecretProvider secrets = new SecretProvider() {
            @Override public String getApiKey() { return API_KEY; }
            @Override public String getApiSecret() { return API_SECRET; }
            @Override public String getDistributorId() { return "TEST_DIST"; }
        };

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeaders(h -> h.setBasicAuth(secrets.getApiKey(), secrets.getApiSecret()))
                .build();

        publisher = new RecordingPublisher();
        client = new OneSbHttpClient(restClient, new OneSbErrorNormaliser(), publisher);
    }

    @Test
    void get_200_sendsBasicAuthAndDeserialisesBody() {
        stubFor(get(urlEqualTo("/v1/probe"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ok\",\"reqId\":\"r-1\"}")));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = client.get("/v1/probe", Map.class);
        assertThat(body).containsEntry("status", "ok");
        verify(exactly(1), getRequestedFor(urlEqualTo("/v1/probe"))
                .withHeader("Authorization", containing("Basic ")));
    }

    @Test
    void get_401_mapsToUpstreamAuthFailure_withoutRetry() {
        stubFor(get(urlEqualTo("/v1/secure"))
                .willReturn(aResponse().withStatus(401).withBody("unauthorized")));
        assertThatThrownBy(() -> client.get("/v1/secure", Map.class))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_AUTH_FAILURE);
                    assertThat(se.isRetryable()).isFalse();
                });
        verify(exactly(1), getRequestedFor(urlEqualTo("/v1/secure")));
    }

    @Test
    void post_exchange_roundTripsJson() {
        stubFor(post(urlEqualTo("/v1/echo"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"accepted\":true}")));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = client.post("/v1/echo", Map.of("ping", "pong"), Map.class);
        assertThat(body).containsEntry("accepted", true);
        verify(exactly(1), postRequestedFor(urlEqualTo("/v1/echo"))
                .withHeader("Authorization", containing("Basic ")));
    }

    @Test
    void timeouts_boundFromOnesbClientProperties() {
        assertThat(properties.connectTimeoutMs()).isEqualTo(3000L);
        assertThat(properties.readTimeoutMs()).isEqualTo(30000L);
        OneSbClientProperties defaults = new OneSbClientProperties(null, 0, 0);
        assertThat(defaults.connectTimeoutMs()).isEqualTo(3000L);
        assertThat(defaults.readTimeoutMs()).isEqualTo(30000L);
    }

    @Test
    void successfulCall_emitsSuccessAuditWithLatencyAndStatus() {
        stubFor(get(urlEqualTo("/v1/probe"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true}")));
        client.get("/v1/probe", Map.class);
        assertThat(publisher.events).hasSize(1);
        AuditEvent event = publisher.events.getFirst();
        assertThat(event.getAction()).isEqualTo(AuditActions.ONESB_OUTBOUND_CALL);
        assertThat(event.getOutcome()).isEqualTo(AuditOutcomes.SUCCESS);
        assertThat(event.getMetadata().get("upstreamHttpStatus")).isEqualTo("200");
        assertThat(Long.parseLong(event.getMetadata().get("latencyMs"))).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void authFailure_emitsFailureAudit() {
        stubFor(get(urlEqualTo("/v1/secure"))
                .willReturn(aResponse().withStatus(401).withBody("unauthorized")));
        assertThatThrownBy(() -> client.get("/v1/secure", Map.class)).isInstanceOf(ServiceException.class);
        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.getFirst().getOutcome()).isEqualTo(AuditOutcomes.FAILURE);
        assertThat(publisher.events.getFirst().getMetadata().get("upstreamHttpStatus")).isEqualTo("401");
    }

    @Test
    void serverError_5xx_emitsFailureAudit() {
        stubFor(get(urlEqualTo("/v1/unstable"))
                .willReturn(aResponse().withStatus(503).withBody("unavailable")));
        assertThatThrownBy(() -> client.get("/v1/unstable", Map.class))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE);
                });
        assertThat(publisher.events).hasSize(1);
        AuditEvent event = publisher.events.getFirst();
        assertThat(event.getAction()).isEqualTo(AuditActions.ONESB_OUTBOUND_CALL);
        assertThat(event.getOutcome()).isEqualTo(AuditOutcomes.FAILURE);
        assertThat(event.getMetadata().get("upstreamHttpStatus")).isEqualTo("503");
        verify(exactly(1), getRequestedFor(urlEqualTo("/v1/unstable")));
    }

    @Test
    void requestHash_isHashOfMaskedBody_notPlaintextPan() throws Exception {
        stubFor(post(urlEqualTo("/v1/quote"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true}")));
        Map<String, String> payload = Map.of("pan", PAN, "name", "Priya Sharma");
        client.post("/v1/quote", payload, Map.class);
        String requestHash = publisher.events.getFirst().getMetadata().get("requestHash");
        String plaintextJson = new ObjectMapper().writeValueAsString(payload);
        String plaintextHash = sha256Hex(plaintextJson);
        assertThat(requestHash).isNotBlank().doesNotContain(PAN);
        assertThat(requestHash).isEqualTo(client.hashMaskedBody(payload));
        assertThat(requestHash).isNotEqualTo(plaintextHash);
        assertThat(PiiMasker.maskJson("{\"pan\":\"" + PAN + "\"}")).doesNotContain(PAN);
    }

    private static String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private static final class RecordingPublisher implements AuditEventPublisher {
        private final List<AuditEvent> events = new ArrayList<>();
        @Override public void publish(AuditEvent event) { events.add(event); }
    }
}
