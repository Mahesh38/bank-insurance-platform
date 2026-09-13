package com.bank.persistence.api.internal.v1;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.test.PyramidTags;
import com.bank.common.test.fixtures.DomainFixtures;
import com.bank.common.test.postgres.PostgresTestSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** S08-E03-S01 — persistence jobs API against real PostgreSQL via shared Testcontainers. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Tag(PyramidTags.INTEGRATION)
@Tag(PyramidTags.TESTCONTAINERS)
class JobApiPostgresIT {

  @Container
  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> POSTGRES = PostgresTestSupport.create();

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    PostgresTestSupport.register(POSTGRES, registry);
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void createJob_againstPostgres_returns201() throws Exception {
    String idem = DomainFixtures.idempotencyKey("pg-job");
    String journey = DomainFixtures.journeyId();

    mockMvc
        .perform(
            post("/internal/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "lob": "TERM",
                                  "jobType": "QUOTE",
                                  "journeyId": "%s",
                                  "idempotencyKey": "%s",
                                  "createdByActor": "%s"
                                }
                                """
                        .formatted(journey, idem, DomainFixtures.actorId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.jobId", notNullValue()))
        .andExpect(jsonPath("$.lob", is("TERM")))
        .andExpect(jsonPath("$.jobType", is("QUOTE")))
        .andExpect(jsonPath("$.status", is("PENDING")))
        .andExpect(jsonPath("$.journeyId", is(journey)))
        .andExpect(jsonPath("$.idempotencyKey", is(idem)));
  }
}
