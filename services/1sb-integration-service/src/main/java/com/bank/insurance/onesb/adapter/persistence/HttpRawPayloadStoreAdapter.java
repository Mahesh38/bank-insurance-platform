package com.bank.insurance.onesb.adapter.persistence;

import com.bank.common.domain.Lob;
import com.bank.common.domain.RawPayloadDirection;
import com.bank.insurance.onesb.adapter.persistence.PersistenceMessages.CreateRawPayloadRequest;
import com.bank.insurance.onesb.domain.port.outbound.RawPayloadCaptureException;
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
 * (COMP-003). Encryption at rest happens server-side; this adapter only transports the plaintext
 * body over the internal network. {@link #store} is best-effort (logged at ERROR, never thrown);
 * {@link #storeEvidence} retries once and then throws, so a proposal or payment request is never
 * sent to 1SB without its evidence (SUG-20261021-gap).
 */
@Component
public class HttpRawPayloadStoreAdapter implements RawPayloadStorePort {

  private static final Logger log = LoggerFactory.getLogger(HttpRawPayloadStoreAdapter.class);

  private final RestClient persistenceRestClient;

  @Autowired
  public HttpRawPayloadStoreAdapter(
      @Qualifier("persistenceRestClient") RestClient persistenceRestClient) {
    this.persistenceRestClient = persistenceRestClient;
  }

  @Override
  public void store(
      String jobId,
      RawPayloadDirection direction,
      String operation,
      Lob lob,
      String payload,
      Integer httpStatus) {
    if (jobId == null || jobId.isBlank() || payload == null) {
      return;
    }
    try {
      post(jobId, direction, operation, lob, payload, httpStatus);
    } catch (Exception e) {
      log.error(
          "RAW_PAYLOAD_CAPTURE_FAILED best-effort (jobId={}, direction={}, operation={}): {}",
          jobId,
          direction,
          operation,
          e.toString());
    }
  }

  @Override
  public void storeEvidence(
      String jobId,
      RawPayloadDirection direction,
      String operation,
      Lob lob,
      String payload,
      Integer httpStatus) {
    if (jobId == null || jobId.isBlank() || payload == null) {
      throw new RawPayloadCaptureException(
          "Evidence without a job id or body cannot be stored", null);
    }
    RuntimeException last = null;
    for (int attempt = 1; attempt <= 2; attempt++) {
      try {
        post(jobId, direction, operation, lob, payload, httpStatus);
        return;
      } catch (RuntimeException e) {
        last = e;
      }
    }
    log.error(
        "RAW_PAYLOAD_CAPTURE_FAILED required (jobId={}, direction={}, operation={}): {}",
        jobId,
        direction,
        operation,
        last.toString());
    throw new RawPayloadCaptureException("Raw payload evidence could not be stored", last);
  }

  private void post(
      String jobId,
      RawPayloadDirection direction,
      String operation,
      Lob lob,
      String payload,
      Integer httpStatus) {
    persistenceRestClient
        .post()
        .uri("/internal/v1/raw-payloads")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            new CreateRawPayloadRequest(
                jobId,
                direction.name(),
                operation,
                lob == null ? "UNKNOWN" : lob.name(),
                payload,
                httpStatus == null ? null : httpStatus.shortValue()))
        .retrieve()
        .toBodilessEntity();
  }
}
