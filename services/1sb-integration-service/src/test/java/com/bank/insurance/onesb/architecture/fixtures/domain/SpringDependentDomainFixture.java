package com.bank.insurance.onesb.architecture.fixtures.domain;

import org.springframework.stereotype.Component;

/**
 * Test-only fixture that DELIBERATELY VIOLATES the rule
 * "domain layer must be pure Java with zero Spring dependencies".
 *
 * <p>Exists to prove the domain-purity rule rejects a real violation.
 */
@Component
public final class SpringDependentDomainFixture {

    public String describe() {
        return "impure";
    }
}
