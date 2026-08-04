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
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API coverage for {@code /internal/v1/raw-payloads} (COMP-003).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class RawPayloadApiTest {

    private static final String RAW_BODY = "{\"applicationNo\":\"APP-RAW-1\",\"customerPan\":\"ABCDE1234F\"}";

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_returns201_withMetadataOnly_noPlaintextLeaked() throws Exception {
        String jobId = createJob();

        mockMvc.perform(post("/internal/v1/raw-payloads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s",
                                  "direction": "RES",
                                  "operation": "POST /v1/quote",
                                  "lob": "TERM",
                                  "payload": %s,
                                  "httpStatus": 200
                                }
                                """.formatted(jobId, objectMapper.writeValueAsString(RAW_BODY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payloadId", notNullValue()))
                .andExpect(jsonPath("$.jobId", is(jobId)))
                .andExpect(jsonPath("$.direction", is("RES")))
                .andExpect(jsonPath("$.operation", is("POST /v1/quote")))
                .andExpect(jsonPath("$.lob", is("TERM")))
                .andExpect(jsonPath("$.httpStatus", is(200)))
                .andExpect(jsonPath("$.retainUntil", notNullValue()))
                .andExpect(jsonPath("$.payload").doesNotExist());
    }

    @Test
    void getById_decryptsAndReturnsOriginalPayload() throws Exception {
        String jobId = createJob();
        String payloadId = createRawPayload(jobId, "REQ", RAW_BODY);

        mockMvc.perform(get("/internal/v1/raw-payloads/{payloadId}", payloadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payloadId", is(payloadId)))
                .andExpect(jsonPath("$.jobId", is(jobId)))
                .andExpect(jsonPath("$.direction", is("REQ")))
                .andExpect(jsonPath("$.payload", is(RAW_BODY)));
    }

    @Test
    void getById_unknown_returns404_withResourceNotFound() throws Exception {
        mockMvc.perform(get("/internal/v1/raw-payloads/{payloadId}", "missing-payload"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code", is(ErrorCodes.RESOURCE_NOT_FOUND)));
    }

    @Test
    void byJobId_listsMetadataOnly_forBothDirections() throws Exception {
        String jobId = createJob();
        createRawPayload(jobId, "REQ", RAW_BODY);
        createRawPayload(jobId, "RES", "{\"status\":\"ok\"}");

        mockMvc.perform(get("/internal/v1/raw-payloads").param("jobId", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].payload").doesNotExist())
                .andExpect(jsonPath("$[1].payload").doesNotExist());
    }

    @Test
    void create_missingRequiredField_returns400() throws Exception {
        mockMvc.perform(post("/internal/v1/raw-payloads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "direction": "REQ",
                                  "operation": "POST /v1/quote",
                                  "lob": "TERM",
                                  "payload": "{}"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.VALIDATION_ERROR)));
    }

    private String createJob() throws Exception {
        MvcResult result = mockMvc.perform(post("/internal/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "jobType": "QUOTE",
                                  "journeyId": "journey-raw",
                                  "idempotencyKey": "idem-raw-%s",
                                  "createdByActor": "raw-payload-test"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("jobId").asText();
    }

    private String createRawPayload(String jobId, String direction, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/internal/v1/raw-payloads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobId": "%s",
                                  "direction": "%s",
                                  "operation": "POST /v1/quote",
                                  "lob": "TERM",
                                  "payload": %s
                                }
                                """.formatted(jobId, direction, objectMapper.writeValueAsString(payload))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("payloadId").asText();
    }
}
