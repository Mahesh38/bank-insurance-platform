package com.bank.platform.integration.hub.application;

import com.bank.platform.integration.hub.domain.CallerRef;
import com.bank.platform.integration.hub.domain.Failure;
import com.bank.platform.integration.hub.domain.QuoteResult;
import java.time.Instant;

/** Poll response view. */
public record ProviderRequestStatusView(
    String providerRequestId,
    String operation,
    String state,
    String provider,
    CallerRef callerRef,
    Instant createdAt,
    QuoteResult result,
    Failure failure) {}
