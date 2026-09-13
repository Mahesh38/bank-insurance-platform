package com.bank.insurance.onesb.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bank-neutral ULIP fund list / performance result. {@code data} is the 1SB payload
 * pass-through — do not interpret fund rows in the bank domain here.
 */
public record UlipFundResult(
        String reqId,
        Map<String, Object> data
) {
    public UlipFundResult {
        data = data == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }
}
