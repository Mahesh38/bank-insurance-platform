package com.bank.common.error;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/** ERR-001 — the enums, the incident id and the origin rule. */
class ErrorTaxonomyTest {

    @Test
    void onlyTransientFailuresAreMachineRetryable() {
        assertThat(Retryability.YES.toBoolean()).isTrue();
        for (Retryability r : Set.of(Retryability.NO, Retryability.AFTER_FIX,
                                     Retryability.AFTER_REMEDIATION, Retryability.AFTER_REAUTH)) {
            assertThat(r.toBoolean())
                .as("%s needs someone to change something first; auto-retry would duplicate", r)
                .isFalse();
        }
    }

    @Test
    void clientCausedCategoriesAreSeparatedFromPlatformCaused() {
        for (ErrorCategory c : Set.of(ErrorCategory.VALIDATION, ErrorCategory.AUTHENTICATION,
                                      ErrorCategory.AUTHORIZATION, ErrorCategory.NOT_FOUND,
                                      ErrorCategory.CONFLICT, ErrorCategory.RATE_LIMIT)) {
            assertThat(c.clientCaused()).as("%s", c).isTrue();
        }
        for (ErrorCategory c : Set.of(ErrorCategory.UPSTREAM, ErrorCategory.CONFIG, ErrorCategory.INTERNAL)) {
            assertThat(c.clientCaused()).as("%s", c).isFalse();
            assertThat(c.alertable()).as("%s", c).isTrue();
        }
    }

    @Test
    void complianceGatesAreAlertableEvenThoughTheCallerCausedThem() {
        assertThat(ErrorCategory.COMPLIANCE_GATE.clientCaused()).isTrue();
        assertThat(ErrorCategory.COMPLIANCE_GATE.alertable())
            .as("a rising compliance-refusal rate is an operational signal")
            .isTrue();
        assertThat(ErrorCategory.VALIDATION.alertable()).isFalse();
    }

    @Test
    void categoryDefaultsAreSelfConsistent() {
        for (ErrorCategory c : ErrorCategory.values()) {
            assertThat(c.defaultHttpStatus()).as("%s", c).isBetween(400, 599);
            assertThat(c.defaultRetryability()).as("%s", c).isNotNull();
            assertThat(c.defaultAudit()).as("%s", c).isNotNull();
        }
    }

    @Test
    void incidentIdsAreWellFormedSortableAndUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 2_000; i++) {
            String id = IncidentId.generate();
            assertThat(IncidentId.isValid(id)).as(id).isTrue();
            assertThat(id).hasSize(26);
            assertThat(seen.add(id)).as("collision on %s", id).isTrue();
        }
    }

    @Test
    void incidentIdsSortInTimeOrder() {
        String earlier = IncidentId.generate(Instant.parse("2026-08-27T09:00:00Z"));
        String later = IncidentId.generate(Instant.parse("2026-08-27T09:00:01Z"));
        assertThat(earlier)
            .as("support searches logs by roughly when it happened; a sortable id keeps that a range scan")
            .isLessThan(later);
    }

    @Test
    void incidentIdValidationRejectsMalformedValues() {
        assertThat(IncidentId.isValid(null)).isFalse();
        assertThat(IncidentId.isValid("")).isFalse();
        assertThat(IncidentId.isValid("TOOSHORT")).isFalse();
        assertThat(IncidentId.isValid("U".repeat(26))).as("U is not in Crockford base-32").isFalse();
        assertThat(IncidentId.isValid("0".repeat(26))).isTrue();
    }

    @Test
    void firstOriginWinsAcrossHops() {
        ErrorOrigin first = ErrorOrigin.of("journey-orchestration", ErrorCodes.SUITABILITY_REQUIRED, PlatformLayer.L6);

        ErrorOrigin afterSecondHop = ErrorOrigin.inherit(first, "onesb", ErrorCodes.UPSTREAM_BAD_RESPONSE, PlatformLayer.L5);
        ErrorOrigin afterThirdHop = ErrorOrigin.inherit(afterSecondHop, "bff", ErrorCodes.UPSTREAM_UNAVAILABLE, PlatformLayer.L4);

        assertThat(afterThirdHop)
            .as("every hop rewriting the origin loses the true source at the second hop")
            .isSameAs(first);
        assertThat(afterThirdHop.service()).isEqualTo("journey-orchestration");
        assertThat(afterThirdHop.layer()).isEqualTo(PlatformLayer.L6);
    }

    @Test
    void inheritCreatesAnOriginWhenThereIsNoneYet() {
        ErrorOrigin created = ErrorOrigin.inherit(null, "onesb", ErrorCodes.QUOTE_EXPIRED, PlatformLayer.L6);
        assertThat(created.service()).isEqualTo("onesb");
        assertThat(created.code()).isEqualTo(ErrorCodes.QUOTE_EXPIRED);
    }

    @Test
    void originRejectsMissingParts() {
        assertThatNullPointerException()
            .isThrownBy(() -> new ErrorOrigin(null, "C", PlatformLayer.L6));
        assertThatNullPointerException()
            .isThrownBy(() -> new ErrorOrigin("s", null, PlatformLayer.L6));
    }
}
