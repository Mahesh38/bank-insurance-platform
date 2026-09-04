package com.bank.common.domain;

import java.util.Map;

/**
 * Pass-through wrapper for a 1SB dynamic proposal form schema.
 * Fields are insurer/product-specific — do not interpret semantics in the bank domain.
 */
public record ProposalSchema(
        Lob lob,
        String productCode,
        String manufacturerId,
        String version,
        Map<String, Object> fields
) {
    public ProposalSchema {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }
}
