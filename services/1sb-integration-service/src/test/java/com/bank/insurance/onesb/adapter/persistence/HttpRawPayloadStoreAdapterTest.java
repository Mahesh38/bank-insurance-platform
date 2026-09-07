package com.bank.insurance.onesb.adapter.persistence;

import com.bank.common.domain.Lob;
import com.bank.common.domain.RawPayloadDirection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Contract tests for {@link HttpRawPayloadStoreAdapter} using MockRestServiceServer.
 */
class HttpRawPayloadStoreAdapterTest {

    private static final String BASE = "http://localhost:8081";

    private MockRestServiceServer server;
    private HttpRawPayloadStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpRawPayloadStoreAdapter(builder.baseUrl(BASE).build());
    }

    @AfterEach
    void verify() {
        server.verify();
    }

    @Test
    void store_postsRawPayloadToPersistence() {
        server.expect(requestTo(BASE + "/internal/v1/raw-payloads"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.jobId").value("job-1"))
                .andExpect(jsonPath("$.direction").value("REQ"))
                .andExpect(jsonPath("$.operation").value("/v1/quote"))
                .andExpect(jsonPath("$.lob").value("TERM"))
                .andExpect(jsonPath("$.payload").value("{\"a\":1}"))
                .andExpect(jsonPath("$.httpStatus").value(200))
                .andRespond(withSuccess("""
                        {"payloadId":"p-1","jobId":"job-1","direction":"REQ","operation":"/v1/quote","lob":"TERM","httpStatus":200,"createdAt":"2026-08-04T00:00:00Z","retainUntil":"2033-08-04"}
                        """, MediaType.APPLICATION_JSON));

        adapter.store("job-1", RawPayloadDirection.REQ, "/v1/quote", Lob.TERM, "{\"a\":1}", 200);
    }

    @Test
    void store_missingLob_sendsUnknown() {
        server.expect(requestTo(BASE + "/internal/v1/raw-payloads"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.lob").value("UNKNOWN"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        adapter.store("job-1", RawPayloadDirection.RES, "/v1/quote", null, "{}", null);
    }

    @Test
    void store_persistenceUnavailable_neverThrows() {
        server.expect(requestTo(BASE + "/internal/v1/raw-payloads"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        adapter.store("job-1", RawPayloadDirection.REQ, "/v1/quote", Lob.TERM, "{}", null);
    }

    @Test
    void store_nullJobId_doesNotCallPersistence() {
        adapter.store(null, RawPayloadDirection.REQ, "/v1/quote", Lob.TERM, "{}", null);
    }

    @Test
    void store_nullPayload_doesNotCallPersistence() {
        adapter.store("job-1", RawPayloadDirection.REQ, "/v1/quote", Lob.TERM, null, null);
    }
}
