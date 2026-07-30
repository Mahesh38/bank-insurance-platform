package com.bank.insurance.onesb.adapter.persistence;

import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Lightweight contract test for {@link HttpJobStoreAdapter} using MockRestServiceServer.
 * Covers create (save) + findById paths against the persistence /internal/v1 API shape.
 */
class HttpJobStoreAdapterTest {

    private static final String BASE = "http://localhost:8081";

    private MockRestServiceServer server;
    private HttpJobStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpJobStoreAdapter(builder.baseUrl(BASE).build());
    }

    @AfterEach
    void verify() {
        server.verify();
    }

    @Test
    void createJob_postsToPersistenceAndReturnsJobId() {
        server.expect(requestTo(BASE + "/internal/v1/jobs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("""
                        {
                          "jobId": "job-123",
                          "jobType": "QUOTE",
                          "lob": "TERM",
                          "status": "PENDING",
                          "journeyId": "j-1",
                          "idempotencyKey": "idem-1",
                          "createdAt": "2026-07-30T12:00:00Z",
                          "updatedAt": "2026-07-30T12:00:00Z",
                          "version": 0,
                          "createdByActor": "actor-1"
                        }
                        """, MediaType.APPLICATION_JSON));

        String jobId = adapter.createJob("TERM", "QUOTE", "j-1", "idem-1", "actor-1");

        assertThat(jobId).isEqualTo("job-123");
    }

    @Test
    void findQuoteJob_getsJobAndOffers() {
        String jobId = "job-123";

        server.expect(requestTo(BASE + "/internal/v1/jobs/" + jobId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "jobId": "job-123",
                          "jobType": "QUOTE",
                          "lob": "TERM",
                          "status": "PENDING",
                          "journeyId": "j-1",
                          "idempotencyKey": "idem-1",
                          "createdAt": "2026-07-30T12:00:00Z",
                          "updatedAt": "2026-07-30T12:00:00Z",
                          "completedAt": null,
                          "version": 0,
                          "createdByActor": "actor-1"
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE + "/internal/v1/jobs/" + jobId + "/offers"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        Optional<QuoteJob> result = adapter.findQuoteJob(jobId);

        assertThat(result).isPresent();
        QuoteJob job = result.get();
        assertThat(job.jobId()).isEqualTo(jobId);
        assertThat(job.status()).isEqualTo(JobStatus.PENDING);
        assertThat(job.lob()).isEqualTo(Lob.TERM);
        assertThat(job.journeyId()).isEqualTo("j-1");
        assertThat(job.offers()).isEmpty();
    }
}
