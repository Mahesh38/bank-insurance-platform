package com.bank.insurance.onesb.adapter.persistence;

import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.model.QuoteOffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Contract tests for {@link HttpJobStoreAdapter} using MockRestServiceServer.
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
    void updateJobPolling_setsRunningAndExternalReqId() {
        server.expect(requestTo(BASE + "/internal/v1/jobs/job-123/status"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.externalReqId").value("REQ-99"))
                .andRespond(withSuccess("""
                        {"jobId":"job-123","status":"RUNNING","externalReqId":"REQ-99"}
                        """, MediaType.APPLICATION_JSON));

        adapter.updateJobPolling("job-123", "REQ-99");
    }

    @Test
    void completeJob_patchesCompletedAndPostsOffers() {
        server.expect(requestTo(BASE + "/internal/v1/jobs/job-123/status"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andRespond(withSuccess("""
                        {"jobId":"job-123","status":"COMPLETED"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE + "/internal/v1/jobs/job-123/offers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.offerId").value("offer-1"))
                .andExpect(jsonPath("$.insurerCode").value("INS1"))
                .andRespond(withStatus(HttpStatus.CREATED).body("""
                        {"offerId":"offer-1","jobId":"job-123"}
                        """).contentType(MediaType.APPLICATION_JSON));

        QuoteOffer offer = new QuoteOffer(
                "offer-1", "INS1", null, "P1", "Product",
                new BigDecimal("100.00"), "YEARLY", new BigDecimal("500000"),
                false, "AVAILABLE", null
        );
        adapter.completeJob("job-123", List.of(offer));
    }

    @Test
    @Tag("FUNC-002")
    void completeJob_mixedOffers_patchesPartial() {
        server.expect(requestTo(BASE + "/internal/v1/jobs/job-partial/status"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andRespond(withSuccess("""
                        {"jobId":"job-partial","status":"PARTIAL"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE + "/internal/v1/jobs/job-partial/offers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.offerId").value("ok-1"))
                .andRespond(withStatus(HttpStatus.CREATED).body("""
                        {"offerId":"ok-1","jobId":"job-partial"}
                        """).contentType(MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE + "/internal/v1/jobs/job-partial/offers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.offerId").value("bad-1"))
                .andExpect(jsonPath("$.errorSummary").value("UW decline"))
                .andRespond(withStatus(HttpStatus.CREATED).body("""
                        {"offerId":"bad-1","jobId":"job-partial"}
                        """).contentType(MediaType.APPLICATION_JSON));

        List<QuoteOffer> mixed = List.of(
                new QuoteOffer(
                        "ok-1", "INS1", null, "P1", "Ok",
                        new BigDecimal("100.00"), "YEARLY", new BigDecimal("500000"),
                        false, "AVAILABLE", null
                ),
                new QuoteOffer(
                        "bad-1", "INS2", null, "P2", "Bad",
                        null, null, null,
                        false, "ERROR", "UW decline"
                )
        );
        adapter.completeJob("job-partial", mixed);
    }

    @Test
    void failJob_withPollTimeout_setsTimeoutStatus() {
        server.expect(requestTo(BASE + "/internal/v1/jobs/job-123/status"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.status").value("TIMEOUT"))
                .andExpect(jsonPath("$.failureReason").value("POLL_TIMEOUT"))
                .andRespond(withSuccess("""
                        {"jobId":"job-123","status":"TIMEOUT","failureReason":"POLL_TIMEOUT"}
                        """, MediaType.APPLICATION_JSON));

        adapter.failJob("job-123", "POLL_TIMEOUT");
    }

    @Test
    void failJob_withOtherReason_setsFailedStatus() {
        server.expect(requestTo(BASE + "/internal/v1/jobs/job-123/status"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("ONE_SB_ERROR"))
                .andRespond(withSuccess("""
                        {"jobId":"job-123","status":"FAILED","failureReason":"ONE_SB_ERROR"}
                        """, MediaType.APPLICATION_JSON));

        adapter.failJob("job-123", "ONE_SB_ERROR");
    }

    @Test
    void recordPollAttempt_postsToPollAttemptsEndpoint() {
        server.expect(requestTo(BASE + "/internal/v1/jobs/job-123/poll-attempts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.attemptNumber").value(2))
                .andExpect(jsonPath("$.httpStatus").value(200))
                .andExpect(jsonPath("$.isComplete").value(false))
                .andExpect(jsonPath("$.durationMs").value(45))
                .andRespond(withStatus(HttpStatus.CREATED).body("""
                        {"attemptId":1,"jobId":"job-123","attemptNumber":2}
                        """).contentType(MediaType.APPLICATION_JSON));

        adapter.recordPollAttempt("job-123", 2, 200, false, 45, null);
    }

    @Test
    void findQuoteJob_getsJobAndOffers_completedWithOffers() {
        String jobId = "job-123";

        server.expect(requestTo(BASE + "/internal/v1/jobs/" + jobId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "jobId": "job-123",
                          "jobType": "QUOTE",
                          "lob": "TERM",
                          "status": "COMPLETED",
                          "journeyId": "j-1",
                          "idempotencyKey": "idem-1",
                          "createdAt": "2026-07-30T12:00:00Z",
                          "updatedAt": "2026-07-30T12:01:00Z",
                          "completedAt": "2026-07-30T12:01:00Z",
                          "version": 1,
                          "createdByActor": "actor-1"
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(BASE + "/internal/v1/jobs/" + jobId + "/offers"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{
                          "offerId": "offer-1",
                          "jobId": "job-123",
                          "insurerCode": "INS1",
                          "productCode": "P1",
                          "productName": "Term",
                          "premiumAmount": 100.00,
                          "premiumFrequency": "YEARLY",
                          "sumAssured": 500000,
                          "outOfBound": false,
                          "offerStatus": "AVAILABLE",
                          "createdAt": "2026-07-30T12:01:00Z"
                        }]
                        """, MediaType.APPLICATION_JSON));

        Optional<QuoteJob> result = adapter.findQuoteJob(jobId);

        assertThat(result).isPresent();
        QuoteJob job = result.get();
        assertThat(job.jobId()).isEqualTo(jobId);
        assertThat(job.status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.lob()).isEqualTo(Lob.TERM);
        assertThat(job.offers()).hasSize(1);
        assertThat(job.offers().getFirst().insurerCode()).isEqualTo("INS1");
    }
}
