package com.bank.insurance.onesb.api;

import java.util.Map;

/** Bank-facing ULIP fund list / performance response. */
public record UlipFundResponse(String reqId, Map<String, Object> data) {}
