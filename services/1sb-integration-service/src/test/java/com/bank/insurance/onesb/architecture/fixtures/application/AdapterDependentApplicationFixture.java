package com.bank.insurance.onesb.architecture.fixtures.application;

import com.bank.insurance.onesb.architecture.fixtures.adapter.FixtureAdapter;

/**
 * Test-only fixture that DELIBERATELY VIOLATES the rule
 * "application layer must not depend on adapters".
 *
 * <p>Its whole purpose is to be rejected. If ArchUnit ever stops flagging this class,
 * the layering rule has silently stopped working and
 * {@code ArchitectureRulesBiteTest} fails — which is the point.
 */
public final class AdapterDependentApplicationFixture {

    private final FixtureAdapter adapter = new FixtureAdapter();

    public String run() {
        return adapter.call();
    }
}
