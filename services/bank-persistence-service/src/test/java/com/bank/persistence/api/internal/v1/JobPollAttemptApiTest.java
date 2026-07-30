package com.bank.persistence.api.internal.v1;

import com.bank.common.error.ErrorCodes;
import com.bank.persistence.repo.JobPollAttemptRepository;
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

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Focused coverage for POST/GET {@code /internal/v1/jobs/{id}/poll-attempts}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class JobPollAttemptApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobPollAttemptRepository pollAttemptRepository;

    @Test
    void addPollAttempt_persistsEntity_andListReturnsIt() throws Exception {
        String jobId = createJob();
        Instant attemptedAt = Instant.parse("2026-07-30T12:00:00Z");

        String body = """
                {
                  "attemptNumber": 1,
                  "attemptedAt": "%s",
                  "httpStatus": 200,
                  "isComplete": false,
                  "durationMs": 42,
                  "errorMessage": null
                }
                """.formatted(attemptedAt);

        MvcResult created = mockMvc.perform(post("/internal/v1/jobs/{jobId}/poll-attempts", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptId", notNullValue()))
                .andExpect(jsonPath("$.jobId", is(jobId)))
                .andExpect(jsonPath("$.attemptNumber", is(1)))
                .andExpect(jsonPath("$.httpStatus", is(200)))
                .andExpect(jsonPath("$.isComplete", is(false)))
                .andExpect(jsonPath("$.durationMs", is(42)))
                .andReturn();

        JsonNode response = objectMapper.readTree(created.getResponse().getContentAsString());
        long attemptId = response.get("attemptId").asLong();

        assertThat(pollAttemptRepository.findById(attemptId)).hasValueSatisfying(entity -> {
            assertThat(entity.getJobId()).isEqualTo(jobId);
            assertThat(entity.getAttemptNumber()).isEqualTo((short) 1);
            assertThat(entity.getHttpStatus()).isEqualTo((short) 200);
            assertThat(entity.getIsComplete()).isFalse();
            assertThat(entity.getDurationMs()).isEqualTo(42);
        });

        mockMvc.perform(get("/internal/v1/jobs/{jobId}/poll-attempts", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].attemptId").value(attemptId))
                .andExpect(jsonPath("$[0].attemptNumber", is(1)));
    }

    @Test
    void addPollAttempt_missingJob_returns404() throws Exception {
        mockMvc.perform(post("/internal/v1/jobs/{jobId}/poll-attempts", "missing-job")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "attemptNumber": 1,
                                  "httpStatus": 200,
                                  "isComplete": false,
                                  "durationMs": 10
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code", is(ErrorCodes.RESOURCE_NOT_FOUND)));
    }

    @Test
    void listPollAttempts_missingJob_returns404() throws Exception {
        mockMvc.perform(get("/internal/v1/jobs/{jobId}/poll-attempts", "missing-job-list"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(ErrorCodes.RESOURCE_NOT_FOUND)));
    }

    private String createJob() throws Exception {
        MvcResult result = mockMvc.perform(post("/internal/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "jobType": "QUOTE",
                                  "journeyId": "journey-1",
                                  "idempotencyKey": "idem-poll-%s",
                                  "createdByActor": "test"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("jobId").asText();
    }
}
