package com.bank.platform.suitability.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hexagonal skeleton fitness tests for bounded context #7 — Suitability & Recommendation.
 *
 * <p>Package selectors use {@code allowEmptyShould(true)} while the scaffold is empty. Remove that
 * flag once domain, application and api layers contain real classes (see TD-007 / S08-E02-S02).
 */
class ServiceArchitectureTest {

    private static final String BASE_PACKAGE = "com.bank.platform.suitability";

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);
    }

    @Test
    void importsNonEmptyPackageTree() {
        assertThat(importedClasses).isNotEmpty();
    }

    @Test
    void applicationMustNotDependOnAdapters() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .allowEmptyShould(true)
            .as("Application layer depends on domain ports, not adapters");

        rule.check(importedClasses);
    }

    @Test
    void domainMustNotDependOnSpring() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .allowEmptyShould(true)
            .as("Domain layer is pure Java");

        rule.check(importedClasses);
    }
}
