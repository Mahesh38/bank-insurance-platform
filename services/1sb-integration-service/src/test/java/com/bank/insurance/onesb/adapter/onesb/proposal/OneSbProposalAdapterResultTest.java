package com.bank.insurance.onesb.adapter.onesb.proposal;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.OneSbProposalSubmitResult;
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

import java.net.http.HttpClient;
import java.time.Duration;
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

/**
 * QA-001 — submit-response parsing and upstream-error remapping for the proposal adapter.
 * <p>
 * {@link OneSbProposalAdapterTest} covers the wire path for the two common submit responses.
 * This class covers the response shapes 1SB varies by product (nested {@code data}, alternate
 * key spellings) and the error-remapping rule that matters to callers: a <em>business</em>
 * rejection becomes non-retryable {@code PROPOSAL_REJECTED}, while a transport or auth failure
 * must keep its original, retryable classification.
 */
@Tag("unit")
@Tag("FUNC-005")
class OneSbProposalAdapterResultTest {

    private static final String SUBMIT_PATH = "/insurance/lifeterm/v1/proposal";

    private static final WireMockServer ONESB = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        ONESB.start();
    }

    @AfterAll
    static void stop() {
        ONESB.stop();
    }

    private RecordingRawPayloadStore rawPayloadStore;
    private OneSbProposalAdapter adapter;

    @BeforeEach
    void setUp() {
        ONESB.resetAll();
        rawPayloadStore = new RecordingRawPayloadStore();
        adapter = new OneSbProposalAdapter(
                new OneSbHttpClient(restClient()), 3600L, new ObjectMapper(), rawPayloadStore);
    }

    private static RestClient restClient() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().baseUrl(ONESB.baseUrl()).requestFactory(factory).build();
    }

    // --- submit result parsing -------------------------------------------------------------

    @Test
    void nullResponseBody_isRejected() {
        assertThatThrownBy(() -> OneSbProposalAdapter.parseSubmitResult(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty body");
    }

    @Test
    void neitherApplicationNumberNorReqId_isRejected() {
        assertThatThrownBy(() -> OneSbProposalAdapter.parseSubmitResult(Map.of("status", "OK")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing both applicationNumber and reqId");
    }

    @Test
    void applicationNumberAtRoot_marksResultComplete() {
        OneSbProposalSubmitResult result =
                OneSbProposalAdapter.parseSubmitResult(Map.of("applicationNumber", "APP-1"));

        assertThat(result.complete()).isTrue();
        assertThat(result.applicationNumber()).isEqualTo("APP-1");
    }

    @Test
    void reqIdOnly_marksResultIncomplete_forPolling() {
        OneSbProposalSubmitResult result =
                OneSbProposalAdapter.parseSubmitResult(Map.of("reqId", "REQ-1"));

        assertThat(result.complete()).isFalse();
        assertThat(result.externalReqId()).isEqualTo("REQ-1");
    }

    @Test
    void alternateApplicationNumberSpellings_areAccepted() {
        assertThat(OneSbProposalAdapter.parseSubmitResult(Map.of("applicationNo", "APP-2"))
                .applicationNumber()).isEqualTo("APP-2");
        assertThat(OneSbProposalAdapter.parseSubmitResult(Map.of("application_number", "APP-3"))
                .applicationNumber()).isEqualTo("APP-3");
    }

    @Test
    void alternateReqIdSpellings_areAccepted() {
        assertThat(OneSbProposalAdapter.parseSubmitResult(Map.of("requestId", "REQ-2"))
                .externalReqId()).isEqualTo("REQ-2");
        assertThat(OneSbProposalAdapter.parseSubmitResult(Map.of("request_id", "REQ-3"))
                .externalReqId()).isEqualTo("REQ-3");
    }

    @Test
    void nestedDataBlock_suppliesReqId() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", Map.of("reqId", "REQ-NESTED"));

        assertThat(OneSbProposalAdapter.parseSubmitResult(body).externalReqId())
                .isEqualTo("REQ-NESTED");
    }

    @Test
    void nestedDataBlock_suppliesApplicationNumber() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", Map.of("applicationNumber", "APP-NESTED", "reqId", "REQ-NESTED"));

        OneSbProposalSubmitResult result = OneSbProposalAdapter.parseSubmitResult(body);

        assertThat(result.applicationNumber()).isEqualTo("APP-NESTED");
        assertThat(result.complete()).isTrue();
    }

    /** A root reqId means the nested block is never consulted — root wins. */
    @Test
    void rootReqId_takesPrecedenceOverNestedData() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reqId", "REQ-ROOT");
        body.put("data", Map.of("reqId", "REQ-NESTED"));

        assertThat(OneSbProposalAdapter.parseSubmitResult(body).externalReqId()).isEqualTo("REQ-ROOT");
    }

    @Test
    void nonMapDataBlock_isIgnored() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("applicationNumber", "APP-4");
        body.put("data", "not-an-object");

        assertThat(OneSbProposalAdapter.parseSubmitResult(body).applicationNumber()).isEqualTo("APP-4");
    }

    @Test
    void blankApplicationNumber_isTreatedAsAbsent() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("applicationNumber", "   ");
        body.put("reqId", "REQ-5");

        OneSbProposalSubmitResult result = OneSbProposalAdapter.parseSubmitResult(body);

        assertThat(result.complete()).isFalse();
        assertThat(result.externalReqId()).isEqualTo("REQ-5");
    }

    // --- upstream error remapping ----------------------------------------------------------

    /** Business rejection → PROPOSAL_REJECTED, non-retryable, field errors carried across. */
    @Test
    void businessRejectionWithFieldErrors_becomesProposalRejected() {
        ONESB.stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"errors":[
                                  {"field":"dob","code":"E11","message":"Age out of range"},
                                  {"field":"sumAssured","code":"E12","message":"Exceeds limit"}
                                ]}
                                """)));

        assertThatThrownBy(() -> adapter.submit("job-1", SUBMIT_PATH, Map.of("a", 1)))
                .isInstanceOfSatisfying(ServiceException.class, ex -> {
                    assertThat(ex.getErrorResponse().getCode()).isEqualTo(ErrorCodes.PROPOSAL_REJECTED);
                    assertThat(ex.getErrorResponse().isRetryable()).isFalse();
                    assertThat(ex.getErrorResponse().getErrors())
                            .extracting(e -> e.field())
                            .containsExactly("dob", "sumAssured");
                    assertThat(ex.getErrorResponse().getErrors())
                            .allMatch(e -> ErrorCodes.PROPOSAL_REJECTED.equals(e.code()));
                });
    }

    /** Auth failure is not a business rejection — it must NOT be remapped. */
    @Test
    void upstreamAuthFailure_keepsItsOriginalClassification() {
        ONESB.stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(401)));

        assertThatThrownBy(() -> adapter.submit("job-2", SUBMIT_PATH, Map.of("a", 1)))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorResponse().getCode())
                                .isEqualTo(ErrorCodes.UPSTREAM_AUTH_FAILURE));
    }

    /** 5xx stays retryable — remapping it to a proposal rejection would strand recoverable work. */
    @Test
    void upstream5xx_staysUpstreamUnavailable() {
        ONESB.stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> adapter.submit("job-3", SUBMIT_PATH, Map.of("a", 1)))
                .isInstanceOfSatisfying(ServiceException.class, ex ->
                        assertThat(ex.getErrorResponse().getCode())
                                .isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE));
    }

    @Test
    void businessRejectionWithoutFieldErrors_stillBecomesProposalRejected() {
        ONESB.stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"Product not available\",\"code\":\"P404\"}")));

        assertThatThrownBy(() -> adapter.submit("job-4", SUBMIT_PATH, Map.of("a", 1)))
                .isInstanceOfSatisfying(ServiceException.class, ex -> {
                    assertThat(ex.getErrorResponse().getCode()).isEqualTo(ErrorCodes.PROPOSAL_REJECTED);
                    assertThat(ex.getErrorResponse().getUpstreamCode()).isEqualTo("P404");
                });
    }

    // --- COMP-003 raw payload capture ------------------------------------------------------

    @Test
    void capturesRequestAndResponseAgainstTheJob() {
        stubSubmitOk();

        adapter.submit("job-cap", SUBMIT_PATH, Map.of("a", 1));

        assertThat(rawPayloadStore.directions())
                .containsExactly(RawPayloadDirection.REQ, RawPayloadDirection.RES);
    }

    @Test
    void nullJobId_skipsCapture_butStillSubmits() {
        stubSubmitOk();

        OneSbProposalSubmitResult result = adapter.submit(null, SUBMIT_PATH, Map.of("a", 1));

        assertThat(result.applicationNumber()).isEqualTo("APP-OK");
        assertThat(rawPayloadStore.calls).isEmpty();
    }

    /** A rejected submit captures the request but never a response — there is no response body. */
    @Test
    void rejectedSubmit_capturesRequestOnly() {
        ONESB.stubFor(post(urlEqualTo(SUBMIT_PATH)).willReturn(aResponse().withStatus(422)));

        assertThatThrownBy(() -> adapter.submit("job-rej", SUBMIT_PATH, Map.of("a", 1)))
                .isInstanceOf(ServiceException.class);

        assertThat(rawPayloadStore.directions()).containsExactly(RawPayloadDirection.REQ);
    }

    @Test
    void captureFailure_doesNotFailTheSubmit() {
        stubSubmitOk();
        OneSbProposalAdapter failing = new OneSbProposalAdapter(
                new OneSbHttpClient(restClient()), 3600L, new ObjectMapper(),
                (jobId, direction, operation, lob, payload, httpStatus) -> {
                    throw new IllegalStateException("persistence down");
                });

        assertThat(failing.submit("job-y", SUBMIT_PATH, Map.of("a", 1)).applicationNumber())
                .isEqualTo("APP-OK");
    }

    // --- cache key -------------------------------------------------------------------------

    @Test
    void cacheKey_distinguishesProductAndVersion_andToleratesNulls() {
        assertThat(OneSbProposalAdapter.cacheKey(Lob.TERM, "T1", "M1", "v1"))
                .isNotEqualTo(OneSbProposalAdapter.cacheKey(Lob.TERM, "T1", "M1", "v2"));
        assertThat(OneSbProposalAdapter.cacheKey(null, null, null, null)).isEqualTo("|||");
    }

    @Test
    void nonPositiveCacheTtl_fallsBackToDefault_withoutThrowing() {
        stubSubmitOk();
        OneSbProposalAdapter zeroTtl = new OneSbProposalAdapter(new OneSbHttpClient(restClient()), 0L);

        assertThat(zeroTtl.submit("job-ttl", SUBMIT_PATH, Map.of("a", 1)).applicationNumber())
                .isEqualTo("APP-OK");
    }

    private static void stubSubmitOk() {
        ONESB.stubFor(post(urlEqualTo(SUBMIT_PATH))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"applicationNumber\":\"APP-OK\"}")));
    }

    private static final class RecordingRawPayloadStore implements RawPayloadStorePort {

        private final List<RawPayloadDirection> calls = new ArrayList<>();

        @Override
        public void store(String jobId, RawPayloadDirection direction, String operation, Lob lob,
                          String payload, Integer httpStatus) {
            calls.add(direction);
        }

        List<RawPayloadDirection> directions() {
            return List.copyOf(calls);
        }
    }
}
