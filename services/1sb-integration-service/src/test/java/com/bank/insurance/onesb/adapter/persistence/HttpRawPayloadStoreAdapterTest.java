package com.bank.insurance.onesb.adapter.persistence;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("COMP-003")
class HttpRawPayloadStoreAdapterTest {

    private static final WireMockServer PERSISTENCE = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        PERSISTENCE.start();
    }

    @AfterAll
    static void stop() {
        PERSISTENCE.stop();
    }

    private HttpRawPayloadStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        PERSISTENCE.resetAll();
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        RestClient client = RestClient.builder()
                .baseUrl(PERSISTENCE.baseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        adapter = new HttpRawPayloadStoreAdapter(client);
    }

    @Test
    void store_postsBase64Payload_andReturnsPayloadId() {
        byte[] plaintext = "{\"quote\":\"secret\"}".getBytes(StandardCharsets.UTF_8);
        String expectedB64 = Base64.getEncoder().encodeToString(plaintext);

        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/raw-payloads"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "payloadId": "payload-1",
                                  "jobId": "job-1",
                                  "direction": "REQ",
                                  "operation": "POST /insurance/lifeterm/v1/quote",
                                  "lob": "TERM",
                                  "encryptionKeyId": "test-v1",
                                  "createdAt": "2026-07-30T12:00:00Z",
                                  "retainUntil": "2033-07-30"
                                }
                                """)));

        String id = adapter.store(
                "job-1",
                "REQ",
                "POST /insurance/lifeterm/v1/quote",
                "TERM",
                plaintext,
                null);

        assertThat(id).isEqualTo("payload-1");

        PERSISTENCE.verify(postRequestedFor(urlEqualTo("/internal/v1/raw-payloads"))
                .withRequestBody(containing("\"jobId\":\"job-1\""))
                .withRequestBody(containing("\"direction\":\"REQ\""))
                .withRequestBody(containing("\"payloadBase64\":\"" + expectedB64 + "\""))
                .withRequestBody(containing("\"lob\":\"TERM\"")));
    }

    @Test
    void store_resWithHttpStatus_includesStatus() {
        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/raw-payloads"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "payloadId": "payload-res-1",
                                  "jobId": "job-2",
                                  "direction": "RES",
                                  "operation": "POST /x",
                                  "lob": "TERM",
                                  "encryptionKeyId": "test-v1",
                                  "httpStatus": 200,
                                  "createdAt": "2026-07-30T12:00:00Z",
                                  "retainUntil": "2033-07-30"
                                }
                                """)));

        String id = adapter.store("job-2", "RES", "POST /x", "TERM",
                "ok".getBytes(StandardCharsets.UTF_8), (short) 200);

        assertThat(id).isEqualTo("payload-res-1");
        PERSISTENCE.verify(postRequestedFor(urlEqualTo("/internal/v1/raw-payloads"))
                .withRequestBody(containing("\"httpStatus\":200")));
    }
}
