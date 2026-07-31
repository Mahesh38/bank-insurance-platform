package com.bank.insurance.onesb.api.v1.dto;

import java.util.List;
import java.util.Map;

/**
 * Bank-facing normalised master lookup response.
 */
public record MasterLookupResponse(
        String lob,
        Map<String, List<CodeLabel>> lookups,
        CacheInfo cache
) {
    public record CodeLabel(String code, String label) {}

    public record CacheInfo(boolean hit, boolean stale) {}
}
