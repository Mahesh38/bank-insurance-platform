package com.bank.insurance.onesb.adapter.persistence;

import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.CreateRawPayloadRequest;
import com.bank.common.domain.Lob;
import com.bank.common.domain.RawPayloadDirection;
import com.bank.insurance.onesb.domain.port.outbound.RawPayloadStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP adapter implementing {@link RawPayloadStorePort} against bank-persistence-service
 * (COMP-003). Encryption at rest happens server-side; this adapter only transports the
 * plaintext body over the internal network. Best-effort: failures are logged, never thrown,
 * so a persistence outage does not fail the caller's business flow.
 */
@Component
public class HttpRawPayloadStoreAdapter implements RawPayloadStorePort {

    private static final Logger log = LoggerFactory.getLogger(HttpRawPayloadStoreAdapter.class);

    private final RestClient persistenceRestClient;

    @Autowired
    public HttpRawPayloadStoreAdapter(@Qualifier("persistenceRestClient") RestClient persistenceRestClient) {
        this.persistenceRestClient = persistenceRestClient;
    }

    @Override
    public void store(String jobId, RawPayloadDirection direction, String operation, Lob lob,
                      String payload, Integer httpStatus) {
        if (jobId == null || jobId.isBlank() || payload == null) {
            return;
        }
        try {
            persistenceRestClient.post()
                    .uri("/internal/v1/raw-payloads")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreateRawPayloadRequest(
                            jobId,
                            direction.name(),
                            operation,
                            lob == null ? "UNKNOWN" : lob.name(),
                            payload,
                            httpStatus == null ? null : httpStatus.shortValue()
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to store raw payload (jobId={}, direction={}, operation={}): {}",
                    jobId, direction, operation, e.toString());
        }
    }
}
