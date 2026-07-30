package com.bank.insurance.onesb.adapter.onesb.proposal;

import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.config.ProposalProperties;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.ProposalSchema;
import com.bank.insurance.onesb.domain.port.outbound.OneSbProposalPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1SB proposal-form adapter — GET dynamic schema via {@link OneSbHttpClient}.
 * <p>
 * <b>Term path (FUNC-004):</b> {@code GET /insurance/lifeterm/v1/proposal/form}
 * with query string built from {@code productCode}, {@code manufacturerId}, and {@code version}
 * (e.g. {@code /insurance/lifeterm/v1/proposal/form?productCode=T1&manufacturerId=HDFC&version=1}).
 * The relative path including query is supplied by the LOB handler; this adapter does not
 * re-resolve product keys. Response body is passed through as {@link ProposalSchema#fields()}
 * without interpreting insurer field semantics. Upstream 5xx is normalised by
 * {@code OneSbErrorNormaliser} to {@code UPSTREAM_UNAVAILABLE} (retryable).
 * <p>
 * In-process cache keyed by {@code lob|productCode|manufacturerId|version} with TTL from
 * {@code insurance.proposals.schema-cache-ttl-seconds} (default 3600).
 */
@Component
public class OneSbProposalAdapter implements OneSbProposalPort {

    private final OneSbHttpClient httpClient;
    private final long schemaCacheTtlSeconds;
    private final ConcurrentHashMap<String, CacheEntry> schemaCache = new ConcurrentHashMap<>();

    @Autowired
    public OneSbProposalAdapter(OneSbHttpClient httpClient, ProposalProperties proposalProperties) {
        this(httpClient, proposalProperties.schemaCacheTtlSeconds());
    }

    /** Test / manual wiring without Spring properties. */
    public OneSbProposalAdapter(OneSbHttpClient httpClient, long schemaCacheTtlSeconds) {
        this.httpClient = httpClient;
        this.schemaCacheTtlSeconds = schemaCacheTtlSeconds > 0 ? schemaCacheTtlSeconds : 3600L;
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
