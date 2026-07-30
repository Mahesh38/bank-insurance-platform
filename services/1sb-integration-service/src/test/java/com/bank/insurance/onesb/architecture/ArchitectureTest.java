package com.bank.insurance.onesb.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit architecture fitness tests enforcing the hexagonal architecture boundaries
 * defined in the 1SB Integration Service architecture document (section 3).
 * <p>
 * These tests run on every PR and fail on any boundary violation.
 * <p>
 * Rules use {@code allowEmptyShould(true)} so that scaffold packages without
 * classes yet (Phase 1) don't cause spurious failures. Once classes are added,
 * any violation will be caught.
 */
class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.bank.insurance.onesb";

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    /**
     * The application layer must not import from any adapter.
     * Use-case orchestrators depend on port interfaces only.
     */
    @Test
    void applicationLayerMustNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .allowEmptyShould(true)
                .as("Application layer must depend only on domain ports, not adapter implementations");

        rule.check(importedClasses);
    }

    /**
     * The API layer must not directly import adapter classes.
     * Controllers must go through the application layer.
     */
    @Test
    void apiLayerMustNotImportAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..api..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .allowEmptyShould(true)
                .as("API controllers must delegate to application services, not adapters directly");

        rule.check(importedClasses);
    }

    /**
     * The domain layer must not import Spring Framework annotations or classes.
     * Domain is pure Java — no Spring dependency.
     */
    @Test
    void domainMustNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..")
                .allowEmptyShould(true)
                .as("Domain layer must be pure Java with zero Spring dependencies");

        rule.check(importedClasses);
    }

    /**
     * The adapter.onesb isolation rule:
     * No class outside adapter.onesb may import classes from adapter.onesb.client.
     * This enforces that 1SB HTTP client types are contained within the adapter boundary.
     */
    @Test
    void onlyOneSbAdapterMayImportOneSbClientPackage() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..adapter.onesb..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter.onesb.client..")
                .allowEmptyShould(true)
                .as("Only adapter.onesb.* is permitted to use the 1SB HTTP client; "
                        + "all other packages must use domain ports");

        rule.check(importedClasses);
    }

    /**
     * The domain layer must not import from adapter packages.
     */
    @Test
    void domainMustNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .allowEmptyShould(true)
                .as("Domain is adapter-agnostic; adapters implement domain ports, not the reverse");

        rule.check(importedClasses);
    }

    /**
     * LOB handlers may only import from domain and adapter.onesb.client — not from other adapters.
     */
    @Test
    void lobHandlersMustNotImportPersistenceOrSecretAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..lob..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter.persistence..")
                .orShould().dependOnClassesThat()
                .resideInAPackage("..adapter.secret..")
                .allowEmptyShould(true)
                .as("LOB handlers interact via domain ports only; "
                        + "they must not directly import persistence or secret adapters");

        rule.check(importedClasses);
    }

    /**
     * Config classes must not be imported by domain or application layer.
     * Config wires things together; domain and application must not depend on it.
     */
    @Test
    void configMustNotBeImportedByDomainOrApplication() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .or().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..config..")
                .allowEmptyShould(true)
                .as("Domain and application layers must not depend on Spring configuration classes");

        rule.check(importedClasses);
    }
}
