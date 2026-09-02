package com.bank.insurance.onesb.architecture.fixtures.errors;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;

/**
 * Deliberately violates {@code noServiceCodeBuildsAnErrorEnvelopeByHand} — it invents a title, a
 * status and a detail instead of taking them from the catalogue, and puts an upstream message
 * straight into {@code detail}.
 *
 * <p>Test-only. {@code ArchitectureTest} imports with {@code DO_NOT_INCLUDE_TESTS}, so this cannot
 * leak into the production rule set.
 */
public final class HandBuiltEnvelopeFixture {

    private HandBuiltEnvelopeFixture() {}

    public static ServiceErrorResponse violate(String upstreamMessage) {
        return ServiceErrorResponse.builder()
                .title("Something Went Wrong")
                .status(499)
                .detail("1SB said: " + upstreamMessage)
                .code(ErrorCodes.INTERNAL_ERROR)
                .build();
    }
}
