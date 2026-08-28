package com.bank.persistence.api.internal.v1;

import com.bank.common.error.ErrorCodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API coverage for {@code /internal/v1/payment-sessions}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class PaymentSessionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPaymentSession_returns201_withExpectedFields() throws Exception {
        String jobId = createJob();
        String sessionId = UUID.randomUUID().toString();

        mockMvc.perform(post("/internal/v1/payment-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "%s",
                                  "jobId": "%s",
                                  "applicationNumber": "APP-SYNTH-1001",
                                  "lob": "TERM",
                                  "paymentUrl": "https://payments.test/session/%s",
                                  "redirectUrl": "https://bank.test/callback",
                                  "status": "CREATED",
                                  "externalTxnId": "txn-synth-1",
                                  "expiresAt": "2026-07-30T18:00:00Z",
                                  "createdByActor": "qa-002-test"
                                }
                                """.formatted(sessionId, jobId, sessionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId", is(sessionId)))
                .andExpect(jsonPath("$.jobId", is(jobId)))
                .andExpect(jsonPath("$.applicationNumber", is("APP-SYNTH-1001")))
                .andExpect(jsonPath("$.lob", is("TERM")))
                .andExpect(jsonPath("$.paymentUrl", is("https://payments.test/session/" + sessionId)))
                .andExpect(jsonPath("$.redirectUrl", is("https://bank.test/callback")))
                .andExpect(jsonPath("$.status", is("CREATED")))
                .andExpect(jsonPath("$.externalTxnId", is("txn-synth-1")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andExpect(jsonPath("$.createdByActor", is("qa-002-test")));
    }

    @Test
    void getPaymentSession_byId_returnsSession() throws Exception {
        String sessionId = createSession();

        mockMvc.perform(get("/internal/v1/payment-sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId", is(sessionId)))
                .andExpect(jsonPath("$.status", is("CREATED")))
                .andExpect(jsonPath("$.applicationNumber", is("APP-SYNTH-2002")));
    }

    @Test
    void getPaymentSession_unknown_returns404_withResourceNotFound() throws Exception {
        mockMvc.perform(get("/internal/v1/payment-sessions/{sessionId}", "missing-session"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code", is(ErrorCodes.RESOURCE_NOT_FOUND)))
                .andExpect(jsonPath("$.title", is("Not found")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    private String createJob() throws Exception {
        MvcResult result = mockMvc.perform(post("/internal/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "jobType": "PAYMENT",
                                  "journeyId": "journey-pay",
                                  "idempotencyKey": "idem-pay-%s",
                                  "createdByActor": "qa-002-test"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("jobId").asText();
    }

    private String createSession() throws Exception {
        String jobId = createJob();
        String sessionId = UUID.randomUUID().toString();
        MvcResult result = mockMvc.perform(post("/internal/v1/payment-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "%s",
                                  "jobId": "%s",
                                  "applicationNumber": "APP-SYNTH-2002",
                                  "lob": "TERM",
                                  "paymentUrl": "https://payments.test/session/%s",
                                  "redirectUrl": "https://bank.test/callback",
                                  "status": "CREATED",
                                  "createdByActor": "qa-002-test"
                                }
                                """.formatted(sessionId, jobId, sessionId)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("sessionId").asText();
    }
}
