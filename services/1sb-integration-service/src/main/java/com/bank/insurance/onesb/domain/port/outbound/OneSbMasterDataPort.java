package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.model.LookupValue;

import java.util.List;
import java.util.Map;

/**
 * Outbound port for 1SB master / enum lookup.
 * Implemented by {@code adapter.onesb}; only that package may hold 1SB types.
 */
public interface OneSbMasterDataPort {

    /**
     * Fetches master lookup values from 1SB for the given entities.
     *
     * @param lob              bank LOB discriminator (cache / routing context; may be unused upstream)
     * @param lookUpCategory   {@code quote} or {@code proposal}
     * @param entityIds        master entity keys (e.g. GENDER, OCC)
     * @param manufacturerId   optional insurer id for proposal-scoped enums
     * @return map of entityId → normalised {@link LookupValue} list
     */
    MasterLookupResult lookup(
            String lob,
            String lookUpCategory,
            List<String> entityIds,
            String manufacturerId);

    /**
     * Normalised lookup payload from the outbound adapter.
     */
    record MasterLookupResult(Map<String, List<LookupValue>> lookups) {}
}
