package com.bank.insurance.onesb.api.dto;

import java.util.Map;

/**
 * Bank-facing ULIP fund list / performance response.
 */
public record UlipFundResponse(
        String reqId,
        Map<String, Object> data
) {}
