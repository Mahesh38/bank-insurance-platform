package com.bank.insurance.onesb.adapter.persistence;

import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.CreateJobRequest;
import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.CreateOfferRequest;
import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.JobResponse;
import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.OfferResponse;
import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.PatchJobStatusRequest;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.model.QuoteOffer;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * HTTP adapter implementing {@link JobStorePort} against bank-persistence-service.
 */
@Component
public class HttpJobStoreAdapter implements JobStorePort {

    private final RestClient persistenceRestClient;

    public HttpJobStoreAdapter(RestClient persistenceRestClient) {
        this.persistenceRestClient = persistenceRestClient;
    }

    @Override
    public String createJob(String lob, String jobType, String journeyId, String idempotencyKey, String actorId) {
        JobResponse response = persistenceRestClient.post()
                .uri("/internal/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateJobRequest(lob, jobType, journeyId, idempotencyKey, actorId))
                .retrieve()
                .body(JobResponse.class);
        if (response == null || response.jobId() == null) {
            throw new IllegalStateException("Persistence createJob returned empty response");
        }
        return response.jobId();
    }

    @Override
    public void updateJobStatus(String jobId, JobStatus status) {
        patchStatus(jobId, new PatchJobStatusRequest(status.name(), null, null, null));
    }

    @Override
    public void updateJobPolling(String jobId, String externalReqId) {
        patchStatus(jobId, new PatchJobStatusRequest(JobStatus.POLLING.name(), null, externalReqId, null));
    }

    @Override
    public void completeJob(String jobId, List<QuoteOffer> offers) {
        Instant completedAt = Instant.now();
        patchStatus(jobId, new PatchJobStatusRequest(JobStatus.COMPLETE.name(), null, null, completedAt));
        if (offers != null) {
            for (QuoteOffer offer : offers) {
                persistenceRestClient.post()
                        .uri("/internal/v1/jobs/{jobId}/offers", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new CreateOfferRequest(
                                offer.offerId(),
                                offer.insurerCode(),
                                offer.productCode(),
                                offer.productName(),
                                offer.premiumAmount(),
                                offer.premiumFrequency(),
                                offer.sumAssured(),
                                offer.outOfBound(),
                                offer.offerStatus(),
                                offer.errorSummary(),
                                null
                        ))
                        .retrieve()
                        .toBodilessEntity();
            }
        }
    }

    @Override
    public void failJob(String jobId, String failureReason) {
        patchStatus(jobId, new PatchJobStatusRequest(
                JobStatus.FAILED.name(),
                failureReason,
                null,
                Instant.now()
        ));
    }

    @Override
    public Optional<QuoteJob> findQuoteJob(String jobId) {
        try {
            JobResponse job = persistenceRestClient.get()
                    .uri("/internal/v1/jobs/{jobId}", jobId)
                    .retrieve()
                    .body(JobResponse.class);
            if (job == null) {
                return Optional.empty();
            }
            List<OfferResponse> offers = persistenceRestClient.get()
                    .uri("/internal/v1/jobs/{jobId}/offers", jobId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<OfferResponse>>() {});
            return Optional.of(toQuoteJob(job, offers != null ? offers : List.of()));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }

    private void patchStatus(String jobId, PatchJobStatusRequest request) {
        persistenceRestClient.patch()
                .uri("/internal/v1/jobs/{jobId}/status", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private static QuoteJob toQuoteJob(JobResponse job, List<OfferResponse> offers) {
        List<QuoteOffer> quoteOffers = offers.stream()
                .map(o -> new QuoteOffer(
                        o.offerId(),
                        o.insurerCode(),
                        null,
                        o.productCode(),
                        o.productName(),
                        o.premiumAmount(),
                        o.premiumFrequency(),
                        o.sumAssured(),
                        Boolean.TRUE.equals(o.outOfBound()),
                        o.offerStatus(),
                        o.errorSummary()
                ))
                .toList();
        return new QuoteJob(
                job.jobId(),
                JobStatus.valueOf(job.status()),
                Lob.valueOf(job.lob()),
                job.journeyId(),
                quoteOffers,
                List.of(),
                job.createdAt(),
                job.completedAt()
        );
    }
}
