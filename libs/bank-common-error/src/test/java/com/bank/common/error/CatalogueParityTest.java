package com.bank.common.error;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * ERR-006 — the registry, the ratified catalogue and the support runbook are one thing.
 *
 * <p>{@code 07-PLATFORM-ERROR-CONTRACT.md §3} promises that "CI diffs the two ... a code in the
 * catalogue with no registry entry fails the build, and vice versa. That is how the catalogue stops
 * being paper." This test is that promise. Without it the three drift apart silently and the
 * drift is discovered mid-incident, which is the worst possible time to learn that the support
 * page for a code does not exist.
 */
class CatalogueParityTest {

    private static final Pattern BACKTICKED_CODE = Pattern.compile("`([A-Z][A-Z0-9_]{4,})`");
    private static final Pattern RUNBOOK_HEADING = Pattern.compile("^### RB-([A-Z0-9_]+)$",
        Pattern.MULTILINE);

    /**
     * Codes catalogue 04 names that the registry deliberately does not carry.
     *
     * <p>Each is a recorded discrepancy in {@code 07-PLATFORM-ERROR-CONTRACT.md §13}, not an
     * oversight. Keeping the list here — rather than loosening the assertion — means adding a new
     * exception is a visible, reviewable act.
     */
    private static final Set<String> CATALOGUE_ONLY = Set.of(
        // §13 row 1: the published constant is IDEMPOTENCY_CONFLICT and wins (partner-consumed).
        "IDEMPOTENCY_KEY_CONFLICT",
        // 04 §7 degraded states: journey states, not refusals. They carry no HTTP status by design.
        "PARTIALLY_QUOTED", "TIMED_OUT", "SUBMISSION_FAILED", "UNCERTAIN",
        "RECONCILIATION_BREAK", "CONFIRMATION_OVERDUE", "ISSUANCE_DISPUTED",
        "MANUAL_INTERVENTION",
        // Referenced as rule / invariant / gate ids in prose, not as wire codes.
        "QUOTE_REJECTED_NO_SUITABILITY", "REJECTED", "SOLD", "DRAFT", "COMPLETED", "PARTIAL",
        "AWAITING_PAYMENT", "INITIATED", "ABANDONED", "SUPERSEDED", "RECONCILED"
    );

    private static Path repoFile(String relative) {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++, dir = dir.getParent()) {
            Path candidate = dir.resolve(relative);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return fail("could not locate " + relative + " from " + Path.of("").toAbsolutePath()
            + " — this test must be able to read the ratified documents it checks against");
    }

    private static String read(String relative) throws IOException {
        return Files.readString(repoFile(relative));
    }

    @Test
    void everyRegisteredCodeSourcedFromCatalogue04ActuallyAppearsInIt() throws IOException {
        String catalogue = read("docs/journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md");

        Set<String> missing = new LinkedHashSet<>();
        for (ErrorDefinition d : ErrorCatalogue.all().values()) {
            if (d.catalogueRef().startsWith("04") && !catalogue.contains(d.code())) {
                missing.add(d.code());
            }
        }

        assertThat(missing)
            .as("these codes claim catalogue 04 as their source but do not appear there — either "
                + "the reference is wrong or the catalogue lost a row")
            .isEmpty();
    }

    @Test
    void everyCodeCatalogue04NamesIsRegisteredOrARecordedException() throws IOException {
        String catalogue = read("docs/journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md");

        Set<String> unregistered = new LinkedHashSet<>();
        Matcher m = BACKTICKED_CODE.matcher(catalogue);
        while (m.find()) {
            String code = m.group(1);
            if (!ErrorCatalogue.isRegistered(code) && !CATALOGUE_ONLY.contains(code)) {
                unregistered.add(code);
            }
        }

        assertThat(unregistered)
            .as("catalogue 04 names these refusals and no service can emit them — the catalogue is "
                + "paper for exactly these codes. Register them, or record them in "
                + "07-PLATFORM-ERROR-CONTRACT.md §13 and add them to CATALOGUE_ONLY")
            .isEmpty();
    }

    @Test
    void everyRegisteredCodeHasASupportRunbookPage() throws IOException {
        String runbook = read("docs/journey-execution/08-SUPPORT-RUNBOOK.md");

        Set<String> documented = new LinkedHashSet<>();
        Matcher m = RUNBOOK_HEADING.matcher(runbook);
        while (m.find()) {
            documented.add(m.group(1));
        }

        assertThat(documented)
            .as("the runbook is generated from the registry; regenerate with "
                + "scripts/support/build-error-runbook.py")
            .containsAll(ErrorCatalogue.codes());

        assertThat(ErrorCatalogue.codes())
            .as("a runbook page for a code no service can emit sends support looking for something "
                + "that cannot happen")
            .containsAll(documented);
    }

    @Test
    void everyRunbookPageNamesWhatSupportMustNeverDo() throws IOException {
        String runbook = read("docs/journey-execution/08-SUPPORT-RUNBOOK.md");

        int pages = 0;
        Matcher m = RUNBOOK_HEADING.matcher(runbook);
        while (m.find()) {
            pages++;
            int start = m.end();
            int nextHeading = runbook.indexOf("\n### ", start);
            String page = runbook.substring(start, nextHeading > 0 ? nextHeading : runbook.length());

            assertThat(page)
                .as("RB-%s must state the L1 action, when to escalate, and the boundary support "
                    + "must not cross — the last is the row that keeps a control from being "
                    + "cleared from a support seat", m.group(1))
                .contains("**L1 action**")
                .contains("**L2 escalation**")
                .contains("**Never**");
        }

        assertThat(pages).isEqualTo(ErrorCatalogue.codes().size());
    }
}
