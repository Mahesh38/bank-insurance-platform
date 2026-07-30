package com.bank.insurance.onesb.application;

import com.bank.insurance.onesb.domain.model.LookupValue;

import java.util.List;
import java.util.Map;

/**
 * Application-layer result for master lookup including cache metadata.
 */
public record MasterLookupOutcome(
        String lob,
        Map<String, List<LookupValue>> lookups,
        boolean cacheHit,
        boolean stale
) {
    public static final String HEADER_HIT = "HIT";
    public static final String HEADER_MISS = "MISS";
    public static final String HEADER_STALE = "STALE";

    public String cacheHeader() {
        if (stale) {
            return HEADER_STALE;
        }
        if (cacheHit) {
            return HEADER_HIT;
        }
        return HEADER_MISS;
    }
}
