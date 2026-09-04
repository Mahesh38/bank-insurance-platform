package com.bank.insurance.onesb.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.common.domain.LookupValue;
import com.bank.insurance.onesb.domain.port.outbound.OneSbMasterDataPort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbMasterDataPort.MasterLookupResult;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Master lookup use-case: in-process TTL cache with stale fallback when 1SB is down.
 */
@Service
public class MasterDataService {

    private final OneSbMasterDataPort masterDataPort;
    private final long cacheTtlSeconds;
    private final Clock clock;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ServiceErrors serviceErrors;


    public MasterDataService(
            OneSbMasterDataPort masterDataPort,
            MastersCacheProperties properties,
            Clock clock,
                          ServiceErrors serviceErrors) {
        this.masterDataPort = masterDataPort;
        this.cacheTtlSeconds = properties.cacheTtlSeconds();
        this.clock = clock;
        this.serviceErrors = serviceErrors;
    }

    public MasterLookupOutcome lookup(
            String lob,
            String lookUpCategory,
            List<String> entityIds,
            String manufacturerId) {
        String key = cacheKey(lob, lookUpCategory, manufacturerId, entityIds);
        CacheEntry existing = cache.get(key);
        Instant now = clock.instant();

        if (existing != null && !existing.isExpired(now, cacheTtlSeconds)) {
            return new MasterLookupOutcome(lob, existing.lookups(), true, false);
        }

        try {
            MasterLookupResult result = masterDataPort.lookup(lob, lookUpCategory, entityIds, manufacturerId);
            Map<String, List<LookupValue>> lookups = copyLookups(result.lookups());
            cache.put(key, new CacheEntry(lookups, now));
            return new MasterLookupOutcome(lob, lookups, false, false);
        } catch (RuntimeException ex) {
            if (existing != null) {
                return new MasterLookupOutcome(lob, existing.lookups(), false, true);
            }
            if (ex instanceof ServiceException se) {
                throw se;
            }
            throw serviceErrors.error(ErrorCodes.UPSTREAM_UNAVAILABLE)
                    .component("MasterDataService")
                    .operation("lookup")
                    .upstream("1SB", null, null)
                    .reason("master lookup upstream unavailable and no cached entry")
                    .cause(ex)
                    .build();
        }
    }

    /** Clears in-process cache (tests / admin). */
    public void clearCache() {
        cache.clear();
    }

    static String cacheKey(String lob, String lookUpCategory, String manufacturerId, List<String> entityIds) {
        String sortedIds = entityIds.stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.joining(","));
        return lob + "|" + lookUpCategory + "|" + (manufacturerId == null ? "" : manufacturerId) + "|" + sortedIds;
    }

    private static Map<String, List<LookupValue>> copyLookups(Map<String, List<LookupValue>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return source.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> List.copyOf(e.getValue() != null ? e.getValue() : List.of())));
    }

    private record CacheEntry(Map<String, List<LookupValue>> lookups, Instant storedAt) {
        boolean isExpired(Instant now, long ttlSeconds) {
            return !now.isBefore(storedAt.plusSeconds(ttlSeconds));
        }
    }
}
