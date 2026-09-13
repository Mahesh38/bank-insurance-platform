package com.bank.insurance.onesb.api.dto;

import java.util.Map;

/**
 * Bank-facing Product UI Data response.
 */
public record ProductUiDataResponse(
        String productId,
        String manufacturerId,
        String reqId,
        Map<String, Object> data
) {}
