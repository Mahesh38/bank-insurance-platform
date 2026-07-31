package com.bank.insurance.onesb.adapter.persistence;

import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.CreateRawPayloadRequest;
import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.RawPayloadResponse;
import com.bank.insurance.onesb.domain.port.outbound.RawPayloadStorePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;

/**
 * HTTP adapter storing encrypted raw payloads via bank-persistence-service
 * {@code POST /internal/v1/raw-payloads}.
 */
@Component
public class HttpRawPayloadStoreAdapter implements RawPayloadStorePort {

    private final RestClient persistenceRestClient;

    @Autowired
    public HttpRawPayloadStoreAdapter(
            @Qualifier("persistenceRestClient") RestClient persistenceRestClient) {
        this.persistenceRestClient = persistenceRestClient;
    }

    @Override
    public String store(
            String jobId,
            String direction,
            String operation,
            String lob,
            byte[] plaintextBytes,
            Short httpStatus) {
        String payloadBase64 = Base64.getEncoder().encodeToString(
                plaintextBytes != null ? plaintextBytes : new byte[0]);

        RawPayloadResponse response = persistenceRestClient.post()
                .uri("/internal/v1/raw-payloads")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateRawPayloadRequest(
                        jobId,
                        direction,
                        operation,
                        lob,
                        payloadBase64,
                        httpStatus
                ))
                .retrieve()
                .body(RawPayloadResponse.class);

        if (response == null || response.payloadId() == null || response.payloadId().isBlank()) {
            throw new IllegalStateException("Persistence createRawPayload returned empty response");
        }
        return response.payloadId();
    }
}
