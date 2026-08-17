package com.bank.insurance.onesb.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <b>Validation test S08-VT-03 — "architecture rules bite".</b>
 * <p>
 * {@link ArchitectureTest} proves the production code has no violations. That is a statement
 * about the code. It is <em>not</em> a statement about the rules: a misconfigured selector, a
 * renamed package or a reinstated {@code allowEmptyShould(true)} would also produce a green
 * run, and nobody would notice.
 * <p>
 * This test closes that gap by evaluating the same rules against fixtures under
 * {@code architecture.fixtures.*} that violate them <b>on purpose</b>, and asserting the rules
 * <b>reject</b> them. If a rule ever stops detecting a real violation, this test fails.
 * <p>
 * The S08 stage file makes the point directly: <i>"a pipeline that runs but never blocks
 * anything is theatre. Each of these tests deliberately breaks a rule and confirms the machine
 * notices — this is the difference between having gates and having gate-shaped
 * configuration."</i>
 * <p>
 * These fixtures are test-only and are never imported by main source. {@link ArchitectureTest}
 * imports with {@code DO_NOT_INCLUDE_TESTS}, so they cannot leak into the production rule set.
 */
class ArchitectureRulesBiteTest {

    private static final String FIXTURE_PACKAGE = "com.bank.insurance.onesb.architecture.fixtures";

    private static JavaClasses fixtureClasses;

    @BeforeAll
    static void importFixtures() {
        // Deliberately WITHOUT DO_NOT_INCLUDE_TESTS — the fixtures are test classes and
        // importing them is the entire point of this test.
        fixtureClasses = new ClassFileImporter().importPackages(FIXTURE_PACKAGE);
    }

    @Test
    void fixturesAreActuallyPresent() {
        // Guards against the worst failure mode of this test: importing nothing, so every
        // "rule rejects the fixture" assertion below would be evaluating against an empty set.
        assertThat(fixtureClasses).as("violation fixtures must be importable").isNotEmpty();
        assertThat(fixtureClasses.stream().map(c -> c.getName()))
                .contains(
                        FIXTURE_PACKAGE + ".adapter.FixtureAdapter",
                        FIXTURE_PACKAGE + ".application.AdapterDependentApplicationFixture",
                        FIXTURE_PACKAGE + ".domain.SpringDependentDomainFixture");
    }

    @Test
    void applicationDependingOnAdapterIsRejected() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .as("Application layer must depend only on domain ports, not adapter implementations");

        assertThatThrownBy(() -> rule.check(fixtureClasses))
                .as("the layering rule must reject a real application -> adapter dependency")
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("AdapterDependentApplicationFixture");
    }

    @Test
    void domainDependingOnSpringIsRejected() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..")
                .as("Domain layer must be pure Java with zero Spring dependencies");

        assertThatThrownBy(() -> rule.check(fixtureClasses))
                .as("the domain-purity rule must reject a real Spring dependency in domain")
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("SpringDependentDomainFixture");
    }

    /**
     * TD-007 regression guard, stated as executable behaviour.
     * <p>
     * With {@code allowEmptyShould(true)}, a rule whose selector matches nothing passes
     * silently. Without it, the same rule fails. This test asserts the second behaviour, so
     * reinstating the flag anywhere — or letting a selector rot after a package rename —
     * becomes a visible failure rather than a permanently green vacuous rule.
     */
    @Test
    void aRuleWhoseSelectorMatchesNothingFailsRatherThanPassingVacuously() {
        ArchRule vacuous = noClasses()
                .that().resideInAPackage("..this.package.does.not.exist..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .as("deliberately unmatched selector");

        assertThatThrownBy(() -> vacuous.check(fixtureClasses))
                .as("TD-007: an empty selector must fail, not pass vacuously")
                .isInstanceOf(AssertionError.class);
    }

    /**
     * The complement of the above: with the flag reinstated the same unmatched rule passes.
     * Documenting both halves makes the consequence of {@code allowEmptyShould(true)} explicit
     * rather than folklore, and explains why it was removed from every production rule.
     */
    @Test
    void allowEmptyShouldTrueIsExactlyWhatMadeVacuousRulesPass() {
        ArchRule vacuousButPermitted = noClasses()
                .that().resideInAPackage("..this.package.does.not.exist..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .allowEmptyShould(true)
                .as("unmatched selector with allowEmptyShould(true)");

        // Passes. This is the behaviour TD-007 removed from the production rules.
        vacuousButPermitted.check(fixtureClasses);
    }
}
