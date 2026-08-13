package com.bank.insurance.onesb.adapter.onesb.payment;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.command.CreatePaymentCommand;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.OneSbPaymentUrlResult;
import com.bank.insurance.onesb.domain.model.RawPayloadDirection;
import com.bank.insurance.onesb.domain.port.outbound.RawPayloadStorePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * QA-001 — optional-field and evidence-capture behaviour for the payment adapter.
 * <p>
 * {@link OneSbPaymentAdapterTest} covers the happy path and the nested-response shape. This
 * class covers what happens when the bank caller omits optional fields (the payload must not
 * carry empty keys upstream), and the COMP-003 raw-payload capture contract: capture is
 * best-effort and must never fail a payment.
 */
@Tag("unit")
@Tag("FUNC-007")
class OneSbPaymentAdapterPayloadTest {

    private static final Instant NOW = Instant.parse("2026-08-13T10:00:00Z");
    private static final Instant EXPIRES = NOW.plusSeconds(3600);

    private static final WireMockServer ONESB = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        ONESB.start();
    }

    @AfterAll
    static void stop() {
        ONESB.stop();
    }

    private RecordingRawPayloadStore rawPayloadStore;
    private OneSbPaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        ONESB.resetAll();
        RestClient restClient = RestClient.builder()
                .baseUrl(ONESB.baseUrl())
                .requestFactory(http1Factory())
                .build();
        SecretProvider secretProvider = mock(SecretProvider.class);
        when(secretProvider.getDistributorId()).thenReturn("BCIBL");
        rawPayloadStore = new RecordingRawPayloadStore();
        adapter = new OneSbPaymentAdapter(
                new OneSbHttpClient(restClient),
                secretProvider,
                Clock.fixed(NOW, ZoneOffset.UTC),
                3600L,
                new ObjectMapper(),
                rawPayloadStore);
    }

    private static JdkClientHttpRequestFactory http1Factory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    private static void stubPaymentUrl() {
        ONESB.stubFor(post(urlEqualTo(OneSbPaymentAdapter.PAYMENT_URL_PATH))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentUrl\":\"https://pay.1sb.test/s/1\",\"transactionId\":\"TXN-1\"}")));
    }

    // --- payload construction -------------------------------------------------------------

    @Test
    void blankCurrency_defaultsToInr() {
        stubPaymentUrl();

        adapter.createPaymentUrl(command("job-1", null, null, null, null));

        assertThat(additionalSetup()).containsEntry("currency", "INR");
    }

    @Test
    void suppliedCurrency_isPassedThrough() {
        stubPaymentUrl();

        adapter.createPaymentUrl(command("job-1", "USD", null, null, null));

        assertThat(additionalSetup()).containsEntry("currency", "USD");
    }

    /** An absent agent must not become an empty {@code agentID} upstream — 1SB rejects blanks. */
    @Test
    void blankAgentId_isOmittedFromDistributorBlock() {
        stubPaymentUrl();

        adapter.createPaymentUrl(command("job-1", null, "   ", null, null));

        assertThat(distributor()).containsEntry("distributorID", "BCIBL").doesNotContainKey("agentID");
    }

    @Test
    void suppliedAgentId_isSentAlongsideDistributorId() {
        stubPaymentUrl();

        adapter.createPaymentUrl(command("job-1", null, "AG-9", null, null));

        assertThat(distributor()).containsEntry("distributorID", "BCIBL").containsEntry("agentID", "AG-9");
    }

    @Test
    void blankJourneyId_isOmitted() {
        stubPaymentUrl();

        adapter.createPaymentUrl(command("job-1", null, null, "", null));

        assertThat(capturedRequest()).doesNotContainKey("UITrackingRefNo");
    }

    @Test
    void suppliedJourneyId_isSentAsUiTrackingRef() {
        stubPaymentUrl();

        adapter.createPaymentUrl(command("job-1", null, null, "j-77", null));

        assertThat(capturedRequest()).containsEntry("UITrackingRefNo", "j-77");
    }

    @Test
    void nullPayer_yieldsEmptyMemberDetails_ratherThanNullKeys() {
        stubPaymentUrl();

        adapter.createPaymentUrl(command("job-1", null, null, null, null));

        assertThat(capturedRequest().get("MemberDetails")).isInstanceOf(Map.class);
        assertThat((Map<?, ?>) capturedRequest().get("MemberDetails")).isEmpty();
    }

    @Test
    void payerDetails_arePassedThrough() {
        stubPaymentUrl();
        CreatePaymentCommand.Payer payer =
                new CreatePaymentCommand.Payer("Asha", "Rao", "9876543210", "asha@example.com");

        adapter.createPaymentUrl(command("job-1", null, null, null, payer));

        @SuppressWarnings("unchecked")
        Map<String, Object> member = (Map<String, Object>) capturedRequest().get("MemberDetails");
        assertThat(member)
                .containsEntry("firstName", "Asha")
                .containsEntry("mobileNumber", "9876543210");
    }

    // --- COMP-003 raw payload capture ----------------------------------------------------

    @Test
    void capturesRequestAndResponseAgainstTheJob() {
        stubPaymentUrl();

        adapter.createPaymentUrl(command("job-cap", null, null, null, null));

        assertThat(rawPayloadStore.directions()).containsExactly(
                RawPayloadDirection.REQ, RawPayloadDirection.RES);
        assertThat(rawPayloadStore.calls).allMatch(c -> "job-cap".equals(c.jobId()));
    }

    /** No job to attach evidence to — capture is skipped, the payment still completes. */
    @Test
    void nullJobId_skipsCapture_butStillReturnsPaymentUrl() {
        stubPaymentUrl();

        OneSbPaymentUrlResult result = adapter.createPaymentUrl(command(null, null, null, null, null));

        assertThat(result.paymentUrl()).isEqualTo("https://pay.1sb.test/s/1");
        assertThat(rawPayloadStore.calls).isEmpty();
    }

    /** NFR-002: a persistence outage must degrade evidence capture, never the payment. */
    @Test
    void captureFailure_doesNotFailThePayment() {
        stubPaymentUrl();
        OneSbPaymentAdapter failing = new OneSbPaymentAdapter(
                new OneSbHttpClient(RestClient.builder()
                        .baseUrl(ONESB.baseUrl()).requestFactory(http1Factory()).build()),
                distributorProvider(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                3600L,
                new ObjectMapper(),
                (jobId, direction, operation, lob, payload, httpStatus) -> {
                    throw new IllegalStateException("persistence down");
                });

        OneSbPaymentUrlResult result = failing.createPaymentUrl(command("job-x", null, null, null, null));

        assertThat(result.paymentUrl()).isEqualTo("https://pay.1sb.test/s/1");
    }

    // --- response parsing ------------------------------------------------------------------

    @Test
    void nullResponseBody_isRejected() {
        assertThatThrownBy(() -> OneSbPaymentAdapter.parseResult(null, EXPIRES))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty body");
    }

    @Test
    void missingPaymentUrl_isRejected() {
        assertThatThrownBy(() -> OneSbPaymentAdapter.parseResult(Map.of("transactionId", "T1"), EXPIRES))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing paymentUrl");
    }

    @Test
    void blankPaymentUrl_isRejected() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentUrl", "   ");

        assertThatThrownBy(() -> OneSbPaymentAdapter.parseResult(body, EXPIRES))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing paymentUrl");
    }

    @Test
    void transactionIdFallsBackAcrossKnownKeyNames() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentUrl", "https://pay/1");
        body.put("referenceId", "REF-9");

        assertThat(OneSbPaymentAdapter.parseResult(body, EXPIRES).externalTxnId()).isEqualTo("REF-9");
    }

    @Test
    void nestedPaymentDetails_suppliesTransactionIdWhenRootHasNone() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("paymentUrl", "https://pay/2");
        nested.put("txnId", "TXN-NESTED");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("PaymentDetails", nested);

        OneSbPaymentUrlResult result = OneSbPaymentAdapter.parseResult(body, EXPIRES);

        assertThat(result.paymentUrl()).isEqualTo("https://pay/2");
        assertThat(result.externalTxnId()).isEqualTo("TXN-NESTED");
    }

    /** Root-level transaction id wins; the nested block only fills a gap. */
    @Test
    void rootTransactionId_isNotOverwrittenByNestedBlock() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("paymentUrl", "https://pay/3");
        nested.put("txnId", "TXN-NESTED");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transactionId", "TXN-ROOT");
        body.put("PaymentDetails", nested);

        assertThat(OneSbPaymentAdapter.parseResult(body, EXPIRES).externalTxnId()).isEqualTo("TXN-ROOT");
    }

    @Test
    void expiryIsCarriedThroughFromTheCaller() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentUrl", "https://pay/4");

        assertThat(OneSbPaymentAdapter.parseResult(body, EXPIRES).expiresAt()).isEqualTo(EXPIRES);
    }

    @Test
    void nonPositiveSessionTtl_fallsBackToOneHour() {
        stubPaymentUrl();
        OneSbPaymentAdapter zeroTtl = new OneSbPaymentAdapter(
                new OneSbHttpClient(RestClient.builder()
                        .baseUrl(ONESB.baseUrl()).requestFactory(http1Factory()).build()),
                distributorProvider(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                0L);

        OneSbPaymentUrlResult result = zeroTtl.createPaymentUrl(command("job-ttl", null, null, null, null));

        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
    }

    // --- helpers ---------------------------------------------------------------------------

    private static SecretProvider distributorProvider() {
        SecretProvider provider = mock(SecretProvider.class);
        when(provider.getDistributorId()).thenReturn("BCIBL");
        return provider;
    }

    private static CreatePaymentCommand command(String jobId, String currency, String agentId,
                                                String journeyId, CreatePaymentCommand.Payer payer) {
        return new CreatePaymentCommand(
                Lob.TERM, "APP-1", jobId, "HDFC", "T1", "https://bank.test/return",
                journeyId, currency, payer, new BigDecimal("4200"), "ANNUAL",
                "rm-1", agentId, "actor-1");
    }

    private Map<String, Object> capturedRequest() {
        RecordingRawPayloadStore.Call request = rawPayloadStore.calls.stream()
                .filter(c -> c.direction() == RawPayloadDirection.REQ)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no request payload captured"));
        try {
            return new ObjectMapper().readValue(request.payload(), Map.class);
        } catch (Exception e) {
            throw new AssertionError("captured payload is not JSON: " + request.payload(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> additionalSetup() {
        return (Map<String, Object>) capturedRequest().get("AdditionalSetup");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> distributor() {
        return (Map<String, Object>) capturedRequest().get("Distributor");
    }

    private static final class RecordingRawPayloadStore implements RawPayloadStorePort {

        private final List<Call> calls = new ArrayList<>();

        @Override
        public void store(String jobId, RawPayloadDirection direction, String operation, Lob lob,
                          String payload, Integer httpStatus) {
            calls.add(new Call(jobId, direction, operation, lob, payload, httpStatus));
        }

        List<RawPayloadDirection> directions() {
            return calls.stream().map(Call::direction).toList();
        }

        private record Call(String jobId, RawPayloadDirection direction, String operation, Lob lob,
                            String payload, Integer httpStatus) {}
    }
}
