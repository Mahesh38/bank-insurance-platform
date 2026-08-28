package com.bank.common.error;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

/**
 * ERR-005 — the registry is the single source of truth for a code's behaviour, and its public text
 * is safe by construction.
 */
class ErrorCatalogueTest {

    @Test
    void everyErrorCodeConstantIsRegistered() throws Exception {
        List<String> unregistered = new ArrayList<>();
        for (Field f : ErrorCodes.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                String code = (String) f.get(null);
                if (!ErrorCatalogue.isRegistered(code)) {
                    unregistered.add(f.getName());
                }
            }
        }
        assertThat(unregistered)
            .as("every ErrorCodes constant needs a catalogue entry, or it is emitted with no "
                + "declared status, wording or retryability")
            .isEmpty();
    }

    @Test
    void constantNameMatchesItsWireValue() throws Exception {
        for (Field f : ErrorCodes.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                assertThat((String) f.get(null))
                    .as("constant %s must equal its wire value — a mismatch is invisible in "
                        + "review and breaks a partner-consumed contract", f.getName())
                    .isEqualTo(f.getName());
            }
        }
    }

    @Test
    void requireThrowsForUnregisteredCode() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ErrorCatalogue.require("NOT_A_REAL_CODE"))
            .withMessageContaining("Unregistered error code")
            .withMessageContaining("04-ERROR-AND-DEGRADED-STATE-CATALOGUE");
    }

    @Test
    void findReturnsEmptyForUnregisteredCode() {
        assertThat(ErrorCatalogue.find("NOT_A_REAL_CODE")).isEmpty();
        assertThat(ErrorCatalogue.find(ErrorCodes.QUOTE_EXPIRED)).isPresent();
    }

    @Test
    void registryIsImmutable() {
        assertThatThrownBy(() -> ErrorCatalogue.all().remove(ErrorCodes.CONFLICT))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(ErrorCatalogue.codes()).contains(ErrorCodes.SUITABILITY_REQUIRED);
    }

    /**
     * S08-G7 support: the public text is a finite set of fixed strings, so "no PII in an error
     * response" is checkable here rather than unprovable across every log statement.
     */
    @Test
    void publicTextCarriesNoInterpolationAndNoPii() {
        Pattern placeholder = Pattern.compile("[{%$]|\\+\\s*\"");
        List<String> banned = List.of(
            "pan", "aadhaar", "dob", "date of birth", "income", "salary",
            "account number", "card number", "otp", "password", "token",
            "http://", "https://", "1sb", "silverbullet", "exception", "sql", "stack");

        for (ErrorDefinition d : ErrorCatalogue.all().values()) {
            for (String text : List.of(d.publicTitle(), d.publicDetail())) {
                assertThat(placeholder.matcher(text).find())
                    .as("%s public text must be a fixed string, not a template: '%s'", d.code(), text)
                    .isFalse();
                String lower = text.toLowerCase(Locale.ROOT);
                for (String bannedTerm : banned) {
                    assertThat(lower)
                        .as("%s public text must not mention '%s'", d.code(), bannedTerm)
                        .doesNotContain(bannedTerm);
                }
            }
        }
    }

    @Test
    void authenticationCausesAreIndistinguishableOnTheWire() {
        // 04 section 3: a response that separates these is a user-enumeration oracle.
        List<String> mustNotDiffer = List.of(
            ErrorCodes.INVALID_STATE,
            ErrorCodes.CODE_ALREADY_CONSUMED,
            ErrorCodes.RETURN_LOCATION_NOT_ALLOWED);

        List<String> details = mustNotDiffer.stream()
            .map(c -> ErrorCatalogue.require(c).publicDetail())
            .distinct()
            .toList();

        assertThat(details)
            .as("the login-ceremony failures must read identically to a client")
            .hasSize(1);
    }

    @Test
    void denyReasonsAreIndistinguishableOnTheWire() {
        // 04 section 4: the DEFAULT_DENY / EXPLICIT_DENY distinction is for audit, not for the RM.
        assertThat(ErrorCatalogue.require(ErrorCodes.DEFAULT_DENY).publicDetail())
            .isEqualTo(ErrorCatalogue.require(ErrorCodes.EXPLICIT_DENY).publicDetail());
    }

    @Test
    void complianceGatesLeaveEvidence() {
        List<String> gates = List.of(
            ErrorCodes.SUITABILITY_REQUIRED,
            ErrorCodes.CONSENT_REQUIRED,
            ErrorCodes.PAYMENT_DEVICE_ISOLATION,
            ErrorCodes.PAYMENT_NOT_RECONCILED);

        for (String code : gates) {
            ErrorDefinition d = ErrorCatalogue.require(code);
            assertThat(d.category()).isEqualTo(ErrorCategory.COMPLIANCE_GATE);
            assertThat(d.audit())
                .as("%s is regulatory evidence and is never a silent refusal", code)
                .isEqualTo(AuditDisposition.COMPLIANCE_EVENT);
            assertThat(d.retryability().toBoolean())
                .as("%s must never be auto-retried by a client", code)
                .isFalse();
        }
    }

    @Test
    void dependencyFailuresWrapAndDoNotPropagate() {
        for (String code : List.of(ErrorCodes.UPSTREAM_UNAVAILABLE, ErrorCodes.UPSTREAM_TIMEOUT,
                                   ErrorCodes.UPSTREAM_BAD_RESPONSE, ErrorCodes.UPSTREAM_AUTH_FAILURE)) {
            assertThat(ErrorCatalogue.require(code).propagation()).isEqualTo(Propagation.WRAP);
        }
        // A gate the RM can clear must reach them as itself, not as a generic 502.
        assertThat(ErrorCatalogue.require(ErrorCodes.SUITABILITY_REQUIRED).propagation())
            .isEqualTo(Propagation.PROPAGATE);
    }

    @Test
    void everyEntryHasARunbookAndACatalogueReference() {
        for (ErrorDefinition d : ErrorCatalogue.all().values()) {
            assertThat(d.runbook()).as("%s runbook", d.code()).isEqualTo("RB-" + d.code());
            assertThat(d.catalogueRef()).as("%s catalogueRef", d.code()).isNotBlank();
            assertThat(d.httpStatus()).as("%s status", d.code()).isBetween(400, 599);
        }
    }

    @Test
    void definitionDefaultsFallBackToItsCategory() {
        ErrorDefinition d = new ErrorDefinition(
            "X", ErrorCategory.VALIDATION, 0, null, "t", "d", null, null, null, "ref");

        assertThat(d.httpStatus()).isEqualTo(ErrorCategory.VALIDATION.defaultHttpStatus());
        assertThat(d.retryability()).isEqualTo(ErrorCategory.VALIDATION.defaultRetryability());
        assertThat(d.audit()).isEqualTo(ErrorCategory.VALIDATION.defaultAudit());
        assertThat(d.propagation()).isEqualTo(Propagation.PROPAGATE);
        assertThat(d.runbook()).isEqualTo("RB-X");
        assertThat(d.alertable()).isFalse();
    }

    @Test
    void definitionRejectsMissingRequiredParts() {
        assertThatNullPointerException().isThrownBy(() -> new ErrorDefinition(
            null, ErrorCategory.VALIDATION, 400, null, "t", "d", null, null, null, "r"));
        assertThatNullPointerException().isThrownBy(() -> new ErrorDefinition(
            "X", null, 400, null, "t", "d", null, null, null, "r"));
        assertThatNullPointerException().isThrownBy(() -> new ErrorDefinition(
            "X", ErrorCategory.VALIDATION, 400, null, null, "d", null, null, null, "r"));
        assertThatNullPointerException().isThrownBy(() -> new ErrorDefinition(
            "X", ErrorCategory.VALIDATION, 400, null, "t", null, null, null, null, "r"));
    }
}
