package com.bank.persistence.api.internal.v1;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API coverage for {@code /internal/v1/audit-events} (synthetic identifiers only — no real PII).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class AuditEventApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void appendAuditEvent_returns201_withExpectedFields() throws Exception {
        String eventId = UUID.randomUUID().toString();
        String resourceId = UUID.randomUUID().toString();

        mockMvc.perform(post("/internal/v1/audit-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "%s",
                                  "eventTime": "2026-07-30T14:00:00Z",
                                  "action": "JOB_CREATED",
                                  "actorId": "actor-synth-001",
                                  "actorType": "SERVICE",
                                  "resourceType": "JOB",
                                  "resourceId": "%s",
                                  "outcome": "SUCCESS",
                                  "lob": "TERM",
                                  "journeyId": "journey-synth-1",
                                  "distributorId": "dist-synth-1",
                                  "agentId": "agent-synth-1",
                                  "metadata": "{\\"source\\":\\"qa-002\\"}",
                                  "traceId": "trace-synth-abc"
                                }
                                """.formatted(eventId, resourceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId", is(eventId)))
                .andExpect(jsonPath("$.action", is("JOB_CREATED")))
                .andExpect(jsonPath("$.actorId", is("actor-synth-001")))
                .andExpect(jsonPath("$.actorType", is("SERVICE")))
                .andExpect(jsonPath("$.resourceType", is("JOB")))
                .andExpect(jsonPath("$.resourceId", is(resourceId)))
                .andExpect(jsonPath("$.outcome", is("SUCCESS")))
                .andExpect(jsonPath("$.lob", is("TERM")))
                .andExpect(jsonPath("$.journeyId", is("journey-synth-1")))
                .andExpect(jsonPath("$.distributorId", is("dist-synth-1")))
                .andExpect(jsonPath("$.agentId", is("agent-synth-1")))
                .andExpect(jsonPath("$.traceId", is("trace-synth-abc")))
                .andExpect(jsonPath("$.eventTime", notNullValue()));
    }

    @Test
    void getAuditEvents_byResourceId_returnsMatchingEvents() throws Exception {
        String resourceId = UUID.randomUUID().toString();
        String eventId = appendEvent(resourceId, "JOB_STATUS_PATCHED");

        mockMvc.perform(get("/internal/v1/audit-events")
                        .param("resourceId", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].eventId", is(eventId)))
                .andExpect(jsonPath("$[0].resourceId", is(resourceId)))
                .andExpect(jsonPath("$[0].action", is("JOB_STATUS_PATCHED")))
                .andExpect(jsonPath("$[0].actorId", is("actor-synth-002")))
                .andExpect(jsonPath("$[0].outcome", is("SUCCESS")));
    }

    @Test
    void getAuditEvents_unknownResource_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/internal/v1/audit-events")
                        .param("resourceId", "no-such-resource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    private String appendEvent(String resourceId, String action) throws Exception {
        String eventId = UUID.randomUUID().toString();
        MvcResult result = mockMvc.perform(post("/internal/v1/audit-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "%s",
                                  "action": "%s",
                                  "actorId": "actor-synth-002",
                                  "actorType": "SERVICE",
                                  "resourceType": "JOB",
                                  "resourceId": "%s",
                                  "outcome": "SUCCESS",
                                  "lob": "TERM"
                                }
                                """.formatted(eventId, action, resourceId)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("eventId").asText();
    }
}
