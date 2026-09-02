package com.bank.common.error;

import com.bank.common.observability.MdcKeys;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>Gate evidence for {@code S08-G7} — "No PII in logs, proven by automated test".</b>
 *
 * <p>The reason this gate was hard before the registry existed: proving "no PII in logs" over a
 * codebase means proving a negative over every log statement anyone will ever write. It cannot be
 * done, so it never was.
 *
 * <p>The registry changes the shape of the problem. Every string a caller can be shown, and every
 * string the platform writes about a refusal, comes from a <b>finite, enumerable set</b> of fixed
 * templates. Asserting over that set is a real proof rather than a sampling exercise — and it
 * fails the build the moment someone adds a template with a customer attribute in it.
 *
 * <p>What this does <em>not</em> prove is that no engineer ever puts a customer name into a
 * {@code reason} at a throw site. That is bounded by review and by
 * {@code ErrorDiagnostic}'s contract, and it is stated here so the gap is visible rather than
 * implied by a green test.
 */
class NoPiiInErrorOutputTest {

    /**
     * Regulated attributes from {@code 07-PLATFORM-ERROR-CONTRACT.md §8}. A term appearing in
     * fixed error text means that attribute is being echoed or hinted at.
     */
    private static final List<String> REGULATED_TERMS = List.of(
        "pan", "aadhaar", "aadhar", "passport", "voter", "dob", "date of birth", "birth",
        "income", "salary", "annual earning", "health", "medical", "diagnosis", "smoker",
        "account number", "card number", "cvv", "ifsc", "otp", "password", "passcode",
        "token", "secret", "api key", "bearer", "mobile number", "email address",
        "address", "pincode", "nominee"
    );

    /** Anything that reveals platform internals rather than customer data. */
    private static final List<String> INTERNAL_TERMS = List.of(
        "1sb", "silverbullet", "keycloak", "postgres", "jdbc", "sql", "exception",
        "stacktrace", "nullpointer", "http://", "https://", "localhost", "127.0.0.1",
        "/v1/", "/internal/", "com.bank"
    );

    /**
     * Matches {@code term} as a word, not a substring.
     *
     * <p>Substring matching reports {@code spanId} as leaking "pan" and {@code company} as leaking
     * "pan" too. A PII check nobody trusts gets deleted, so it has to be right about the boring
     * cases before it is useful about the real ones.
     */
    private static boolean mentions(String text, String term) {
        return Pattern.compile("\\b" + Pattern.quote(term) + "\\b", Pattern.CASE_INSENSITIVE)
            .matcher(text)
            .find();
    }

    /** Splits {@code camelCaseKeys} into words so a key can be checked term by term. */
    private static String words(String camelCase) {
        return camelCase.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
    }

    @Test
    void noCallerFacingTextNamesARegulatedAttribute() {
        for (ErrorDefinition d : ErrorCatalogue.all().values()) {
            for (String text : List.of(d.publicTitle(), d.publicDetail())) {
                for (String term : REGULATED_TERMS) {
                    assertThat(mentions(text, term))
                        .as("%s public text names the regulated attribute '%s': \"%s\"",
                            d.code(), term, text)
                        .isFalse();
                }
            }
        }
    }

    @Test
    void noCallerFacingTextRevealsPlatformInternals() {
        for (ErrorDefinition d : ErrorCatalogue.all().values()) {
            for (String text : List.of(d.publicTitle(), d.publicDetail())) {
                String lower = text.toLowerCase(Locale.ROOT);
                for (String term : INTERNAL_TERMS) {
                    assertThat(lower)
                        .as("%s public text reveals '%s': \"%s\"", d.code(), term, text)
                        .doesNotContain(term);
                }
            }
        }
    }

    @Test
    void callerFacingTextIsFixedSoItCannotInterpolateAValue() {
        for (ErrorDefinition d : ErrorCatalogue.all().values()) {
            for (String text : List.of(d.publicTitle(), d.publicDetail())) {
                assertThat(text)
                    .as("%s public text must be a constant — a placeholder is a hole a customer "
                        + "attribute eventually falls through", d.code())
                    .doesNotContain("{").doesNotContain("}")
                    .doesNotContain("%s").doesNotContain("%d").doesNotContain("$");
            }
        }
    }

    @Test
    void aRedactedResponseCarriesNothingBeyondTheSafeEnvelope() {
        // The worst case: a diagnostic packed with everything an engineer could want.
        ServiceException loaded = ServiceException.of(ErrorCodes.UPSTREAM_BUSINESS_ERROR)
            .service("onesb")
            .layer(PlatformLayer.L5)
            .component("OneSbProposalAdapter")
            .operation("POST /v1/proposals")
            .upstream("1SB", "ERR_PAN_INVALID", 422)
            .reason("PAN AAAAA1111A rejected for customer Priya Sharma, dob 1990-01-01")
            .cause(new IllegalStateException("jdbc connection to postgres://vault failed"))
            .build();

        ServiceErrorResponse published = loaded.getErrorResponse().toPublic();

        String serialisedShape = String.join("|",
            String.valueOf(published.getTitle()),
            String.valueOf(published.getDetail()),
            String.valueOf(published.getType()),
            String.valueOf(published.getCode()));

        for (String term : REGULATED_TERMS) {
            assertThat(mentions(serialisedShape, term))
                .as("redacted response leaked '%s'", term).isFalse();
        }
        String lower = serialisedShape.toLowerCase(Locale.ROOT);
        assertThat(lower).doesNotContain("priya").doesNotContain("aaaaa1111a")
            .doesNotContain("1990").doesNotContain("postgres").doesNotContain("jdbc");

        assertThat(published.getDiagnostic()).isNull();
        assertThat(published.getOrigin()).isNull();

        // ...and the engineer half still holds every bit of it, reachable by the incident id.
        assertThat(loaded.getDiagnostic().getReason()).contains("Priya Sharma");
        assertThat(published.getIncidentId()).isEqualTo(loaded.getIncidentId());
    }

    @Test
    void mdcCarriesOnlyBoundedIdentifiersNeverCustomerAttributes() throws Exception {
        // MDC lands in the log index. A key named after a customer attribute would put that
        // attribute in the index by design rather than by accident.
        for (Field f : MdcKeys.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                String key = words((String) f.get(null));
                for (String term : REGULATED_TERMS) {
                    assertThat(mentions(key, term))
                        .as("MDC key %s ('%s') would place '%s' in the log index",
                            f.getName(), key, term)
                        .isFalse();
                }
            }
        }
    }

    @Test
    void theActorReferenceIsPseudonymousByContract() {
        // ErrorDiagnostic exposes no raw actor identifier: the field does not exist, so a throw
        // site cannot populate one by habit.
        assertThat(ErrorDiagnostic.class.getDeclaredFields())
            .extracting(Field::getName)
            .as("a raw actor id on the diagnostic would be copied into every log line")
            .doesNotContain("actorId", "customerName", "pan", "mobile", "email");
    }
}
