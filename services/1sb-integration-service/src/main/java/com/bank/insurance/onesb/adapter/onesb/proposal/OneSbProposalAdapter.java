package com.bank.insurance.onesb.adapter.onesb.proposal;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.config.ProposalProperties;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.OneSbProposalSubmitResult;
import com.bank.insurance.onesb.domain.model.ProposalSchema;
import com.bank.insurance.onesb.domain.model.RawPayloadDirection;
import com.bank.insurance.onesb.domain.port.outbound.OneSbProposalPort;
import com.bank.insurance.onesb.domain.port.outbound.RawPayloadStorePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1SB proposal adapter — GET dynamic schema + POST submit via {@link OneSbHttpClient}.
 * <p>
 * <b>Term paths:</b>
 * <ul>
 *   <li>Schema (FUNC-004): {@code GET /insurance/lifeterm/v1/proposal/form?...}</li>
 *   <li>Submit (FUNC-005): {@code POST /insurance/lifeterm/v1/proposal}</li>
 * </ul>
 * Upstream 4xx business errors on submit are remapped to {@code PROPOSAL_REJECTED}.
 * Upstream 5xx remains {@code UPSTREAM_UNAVAILABLE} (retryable).
 * <p>
 * Submit request/response is captured as raw payload evidence (COMP-003) — best-effort, never
 * fails the proposal flow.
 */
@Component
public class OneSbProposalAdapter implements OneSbProposalPort {

    private final OneSbHttpClient httpClient;
    private final long schemaCacheTtlSeconds;
    private final ObjectMapper objectMapper;
    private final RawPayloadStorePort rawPayloadStorePort;
    private final ConcurrentHashMap<String, CacheEntry> schemaCache = new ConcurrentHashMap<>();

    @Autowired
    public OneSbProposalAdapter(OneSbHttpClient httpClient, ProposalProperties proposalProperties,
                                ObjectMapper objectMapper, RawPayloadStorePort rawPayloadStorePort) {
        this(httpClient, proposalProperties.schemaCacheTtlSeconds(), objectMapper, rawPayloadStorePort);
    }

    /** Test / manual wiring without Spring properties or raw payload capture. */
    public OneSbProposalAdapter(OneSbHttpClient httpClient, long schemaCacheTtlSeconds) {
        this(httpClient, schemaCacheTtlSeconds, new ObjectMapper(),
                (jobId, direction, operation, lob, payload, httpStatus) -> { });
    }

    public OneSbProposalAdapter(OneSbHttpClient httpClient, long schemaCacheTtlSeconds,
                                ObjectMapper objectMapper, RawPayloadStorePort rawPayloadStorePort) {
        this.httpClient = httpClient;
        this.schemaCacheTtlSeconds = schemaCacheTtlSeconds > 0 ? schemaCacheTtlSeconds : 3600L;
        this.objectMapper = objectMapper;
        this.rawPayloadStorePort = rawPayloadStorePort;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ProposalSchema getSchema(Lob lob, String productCode, String manufacturerId,
                                    String version, String path) {
        String cacheKey = cacheKey(lob, productCode, manufacturerId, version);
        CacheEntry cached = schemaCache.get(cacheKey);
        if (cached != null && !cached.isExpired(Instant.now())) {
            return cached.schema();
        }

        Map<String, Object> body = httpClient.get(path, Map.class);
        Map<String, Object> fields = body == null ? Map.of() : new LinkedHashMap<>(body);
        ProposalSchema schema = new ProposalSchema(lob, productCode, manufacturerId, version, fields);
        schemaCache.put(cacheKey, new CacheEntry(schema, Instant.now().plusSeconds(schemaCacheTtlSeconds)));
        return schema;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OneSbProposalSubmitResult submit(String jobId, String path, Object payload) {
        captureRaw(jobId, RawPayloadDirection.REQ, path, payload);
        Map<String, Object> response;
        try {
            response = httpClient.post(path, payload, Map.class);
        } catch (ServiceException ex) {
            throw remapBusinessReject(ex);
        }
        captureRaw(jobId, RawPayloadDirection.RES, path, response);
        return parseSubmitResult(response);
    }

    private void captureRaw(String jobId, RawPayloadDirection direction, String operation, Object body) {
        if (jobId == null || body == null) {
            return;
        }
        try {
            String json = body instanceof String s ? s : objectMapper.writeValueAsString(body);
            rawPayloadStorePort.store(jobId, direction, operation, null, json, null);
        } catch (Exception ignored) {
            // best-effort — never block the proposal flow on capture failure
        }
    }

    static OneSbProposalSubmitResult parseSubmitResult(Map<String, Object> response) {
        if (response == null) {
            throw new IllegalStateException("1SB proposal submit returned empty body");
        }
        String applicationNumber = firstText(response,
                "applicationNumber", "applicationNo", "application_number");
        String reqId = firstText(response, "reqId", "requestId", "request_id");
        if (reqId == null && response.get("data") instanceof Map<?, ?> data) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            if (applicationNumber == null) {
                applicationNumber = firstText(dataMap,
                        "applicationNumber", "applicationNo", "application_number");
            }
            if (reqId == null) {
                reqId = firstText(dataMap, "reqId", "requestId", "request_id");
            }
        }
        boolean complete = StringUtils.hasText(applicationNumber);
        if (!complete && !StringUtils.hasText(reqId)) {
            throw new IllegalStateException(
                    "1SB proposal submit missing both applicationNumber and reqId");
        }
        return new OneSbProposalSubmitResult(reqId, applicationNumber, complete);
    }

    private static ServiceException remapBusinessReject(ServiceException ex) {
        ServiceErrorResponse upstream = ex.getErrorResponse();
        if (upstream == null) {
            return ex;
        }
        String code = upstream.getCode();
        if (!ErrorCodes.UPSTREAM_BUSINESS_ERROR.equals(code)) {
            return ex;
        }
        ServiceErrorResponse.ServiceErrorResponseBuilder builder = ServiceErrorResponse.builder()
                .title("Proposal Rejected")
                .status(422)
                .detail(upstream.getDetail() != null ? upstream.getDetail() : "1SB rejected the proposal")
                .code(ErrorCodes.PROPOSAL_REJECTED)
                .retryable(false)
                .upstreamCode(upstream.getUpstreamCode());
        if (upstream.getErrors() != null) {
            for (ServiceError error : upstream.getErrors()) {
                builder.addError(ServiceError.ofField(
                        ErrorCodes.PROPOSAL_REJECTED,
                        error.message(),
                        error.field()));
            }
        }
        return new ServiceException(builder.build(), ex);
    }

    private static String firstText(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null) {
                String s = v.toString();
                if (StringUtils.hasText(s)) {
                    return s;
                }
            }
        }
        return null;
    }

    static String cacheKey(Lob lob, String productCode, String manufacturerId, String version) {
        return String.join("|",
                lob == null ? "" : lob.name(),
                nullToEmpty(productCode),
                nullToEmpty(manufacturerId),
                nullToEmpty(version));
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private record CacheEntry(ProposalSchema schema, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return !now.isBefore(expiresAt);
        }
    }
}
