package com.bank.insurance.onesb.adapter.onesb.client;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.adapter.onesb.error.OneSbErrorNormaliser;
import com.bank.insurance.onesb.adapter.onesb.resilience.OneSbCircuitBreaker;
import com.bank.insurance.onesb.adapter.onesb.resilience.OneSbCircuitBreakerProperties;
import com.bank.insurance.onesb.observability.PiiMasker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Single HTTP stack for all outbound 1SB calls (Basic Auth, timeouts, no 401 retry).
 * Emits outbound audit events with a hash of the <em>masked</em> request body.
 * Circuit breaker ({@code NFR-004}) fails fast with {@link ErrorCodes#UPSTREAM_UNAVAILABLE}.
 */
@Component
public class OneSbHttpClient {

    private static final Logger log = LoggerFactory.getLogger(OneSbHttpClient.class);

    private final RestClient restClient;
    private final OneSbErrorNormaliser errorNormaliser;
    private final AuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;
    private final ServiceErrors serviceErrors;
    private final OneSbCircuitBreaker circuitBreaker;

    @Autowired
    public OneSbHttpClient(
            @Qualifier("oneSbRestClient") RestClient oneSbRestClient,
            OneSbErrorNormaliser errorNormaliser,
            AuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper,
            ServiceErrors serviceErrors,
            OneSbCircuitBreaker circuitBreaker) {
        this.restClient = oneSbRestClient;
        this.errorNormaliser = errorNormaliser;
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
        this.serviceErrors = serviceErrors;
        this.circuitBreaker = circuitBreaker != null ? circuitBreaker : disabledBreaker();
    }

    /** Convenience for tests / temporary poll adapters. */
    public OneSbHttpClient(RestClient oneSbRestClient) {
        this(oneSbRestClient, ServiceErrors.of("onesb", PlatformLayer.L5));
    }

    /** Convenience for tests / temporary poll adapters, with an explicit identity. */
    public OneSbHttpClient(RestClient oneSbRestClient, ServiceErrors serviceErrors) {
        this(oneSbRestClient, new OneSbErrorNormaliser(serviceErrors), event -> {}, new ObjectMapper(),
             serviceErrors, disabledBreaker());
    }

    public OneSbHttpClient(
            RestClient oneSbRestClient,
            OneSbErrorNormaliser errorNormaliser,
            AuditEventPublisher auditEventPublisher,
            ServiceErrors serviceErrors) {
        this(oneSbRestClient, errorNormaliser, auditEventPublisher, new ObjectMapper(), serviceErrors,
                disabledBreaker());
    }

    private static OneSbCircuitBreaker disabledBreaker() {
        return new OneSbCircuitBreaker(new OneSbCircuitBreakerProperties(false, 5, 30_000L, 1));
    }

    public <T> T get(String path, Class<T> responseType) {
        return exchange(HttpMethod.GET, path, null, responseType);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return exchange(HttpMethod.POST, path, body, responseType);
    }

    public <T> T exchange(HttpMethod method, String path, Object body, Class<T> responseType) {
        if (!circuitBreaker.allowRequest()) {
            throw serviceErrors.error(ErrorCodes.UPSTREAM_UNAVAILABLE)
                    .component("OneSbHttpClient")
                    .operation(method + " " + path)
                    .upstream(OneSbErrorNormaliser.UPSTREAM, "CIRCUIT_OPEN", 503)
                    .reason("1SB circuit breaker open")
                    .build();
        }
        log.debug("1SB {} {}", method, path);
        long started = System.nanoTime();
        Integer upstreamStatus = null;
        String outcome = AuditOutcomes.FAILURE;
        try {
            RestClient.RequestBodySpec spec = restClient.method(method).uri(path);
            if (body != null) {
                spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            ResponseEntity<T> entity = spec.retrieve()
                    .onStatus(status -> status.value() == 401, (request, response) -> {
                        throw errorNormaliser.normalise(401, readBody(response));
                    })
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw errorNormaliser.normalise(
                                response.getStatusCode().value(), readBody(response));
                    })
                    .toEntity(responseType);
            upstreamStatus = entity.getStatusCode().value();
            outcome = AuditOutcomes.SUCCESS;
            circuitBreaker.recordSuccess();
            return entity.getBody();
        } catch (ServiceException ex) {
            upstreamStatus = inferUpstreamStatus(ex, upstreamStatus);
            recordBreakerFailure(ex);
            throw ex;
        } catch (RestClientResponseException ex) {
            upstreamStatus = ex.getStatusCode().value();
            ServiceException normalised = errorNormaliser.normalise(
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            recordBreakerFailure(normalised);
            throw normalised;
        } catch (Exception ex) {
            if (ex.getCause() instanceof ServiceException se) {
                upstreamStatus = inferUpstreamStatus(se, upstreamStatus);
                recordBreakerFailure(se);
                throw se;
            }
            circuitBreaker.recordFailure();
            throw serviceErrors.error(ErrorCodes.UPSTREAM_UNAVAILABLE)
                    .component("OneSbHttpClient")
                    .operation(method + " " + path)
                    .upstream(OneSbErrorNormaliser.UPSTREAM, null, upstreamStatus)
                    .reason("1SB call failed: " + method + " " + path)
                    .cause(ex)
                    .build();
        } finally {
            long latencyMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
            publishAudit(method, path, body, latencyMs, upstreamStatus, outcome);
        }
    }

    private void recordBreakerFailure(ServiceException ex) {
        // Auth failures must not open the breaker (credentials / allowlist — ops action).
        if (ErrorCodes.UPSTREAM_AUTH_FAILURE.equals(ex.getErrorResponse().getCode())) {
            return;
        }
        circuitBreaker.recordFailure();
    }

    private void publishAudit(
            HttpMethod method,
            String path,
            Object body,
            long latencyMs,
            Integer upstreamStatus,
            String outcome) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .actorId("1sb-integration-service")
                    .actorType("SERVICE")
                    .action(AuditActions.ONESB_OUTBOUND_CALL)
                    .resourceType("ONESB_HTTP")
                    .resourceId(path)
                    .outcome(outcome)
                    .metadata("operation", method.name() + " " + path)
                    .metadata("latencyMs", Long.toString(latencyMs))
                    .metadata("upstreamHttpStatus",
                            upstreamStatus == null ? "unknown" : Integer.toString(upstreamStatus))
                    .metadata("requestHash", hashMaskedBody(body))
                    .build();
            auditEventPublisher.publish(event);
        } catch (Exception e) {
            log.warn("Failed to publish outbound 1SB audit event for {} {}: {}", method, path, e.toString());
        }
    }

    String hashMaskedBody(Object body) {
        if (body == null) {
            return sha256Hex("");
        }
        String raw;
        if (body instanceof String s) {
            raw = s;
        } else {
            try {
                raw = objectMapper.writeValueAsString(body);
            } catch (JsonProcessingException e) {
                raw = String.valueOf(body);
            }
        }
        String masked = looksLikeJson(raw) ? PiiMasker.maskJson(raw) : PiiMasker.maskText(raw);
        return sha256Hex(masked);
    }

    private static boolean looksLikeJson(String raw) {
        String t = raw.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static Integer inferUpstreamStatus(ServiceException ex, Integer fallback) {
        String code = ex.getErrorResponse().getCode();
        if ("UPSTREAM_AUTH_FAILURE".equals(code)) {
            return 401;
        }
        if ("UPSTREAM_UNAVAILABLE".equals(code)) {
            return 503;
        }
        if ("UPSTREAM_BUSINESS_ERROR".equals(code)) {
            return 422;
        }
        return fallback;
    }

    private static String readBody(org.springframework.http.client.ClientHttpResponse response) {
        try {
            byte[] bytes = response.getBody().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
