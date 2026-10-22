package com.bank.platform.adminconfig.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Hexagonal skeleton fitness tests for bounded context #19 — Administration & Config.
 *
 * <p>The layers now hold real classes (ADR-024 rule-set store); {@code allowEmptyShould} remains
 * only on the original scaffold rules.
 */
class ServiceArchitectureTest {

  private static final String BASE_PACKAGE = "com.bank.platform.adminconfig";

  private static JavaClasses importedClasses;

  @BeforeAll
  static void importClasses() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);
  }

  @Test
  void importsNonEmptyPackageTree() {
    assertThat(importedClasses).isNotEmpty();
  }

  @Test
  void applicationMustNotDependOnAdapters() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .allowEmptyShould(true)
            .as("Application layer depends on domain ports, not adapters");

    rule.check(importedClasses);
  }

  @Test
  void domainMustNotDependOnAdaptersOrApi() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..adapter..", "..api..", "..application..")
            .as("Domain depends on nothing outward (rule engine core is pure Java)");

    rule.check(importedClasses);
  }

  @Test
  void domainMustNotDependOnSpring() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .allowEmptyShould(true)
            .as("Domain layer is pure Java");

    rule.check(importedClasses);
  }
}
