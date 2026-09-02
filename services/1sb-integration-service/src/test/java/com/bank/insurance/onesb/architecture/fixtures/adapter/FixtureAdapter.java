package com.bank.insurance.onesb.architecture.fixtures.adapter;

/**
 * Test-only fixture. Resides in a package matching the {@code ..adapter..} selector so that
 * {@link com.bank.insurance.onesb.architecture.ArchitectureRulesBiteTest} can prove the
 * layering rules reject a real violation.
 *
 * <p>Never referenced by main source. Excluded from the production rule set because
 * {@code ArchitectureTest} imports with {@code DO_NOT_INCLUDE_TESTS}.
 */
public final class FixtureAdapter {

    public String call() {
        return "fixture";
    }
}
