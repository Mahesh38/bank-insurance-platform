package com.bank.insurance.onesb.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bank-neutral Product UI Data result. {@code data} is the 1SB payload pass-through
 * (insurer/product specific) — do not interpret it in the bank domain.
 */
public record ProductUiData(
        String productId,
        String manufacturerId,
        String reqId,
        Map<String, Object> data
) {
    public ProductUiData {
        data = data == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }
}
