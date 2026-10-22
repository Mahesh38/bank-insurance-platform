package com.bank.platform.consent.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Hexagonal boundaries for consent. Domain stays free of Spring. Callers depend on ports. */
@Tag("unit")
class ServiceArchitectureTest {

  private static final String BASE_PACKAGE = "com.bank.platform.consent";

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
            .because("Application layer depends on domain ports, not adapters");
    rule.check(importedClasses);
  }

  @Test
  void apiMustNotDependOnAdapters() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .because("API layer talks to the application service");
    rule.check(importedClasses);
  }

  @Test
  void domainMustNotDependOnSpringOrAdapters() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "..adapter..")
            .because("Domain layer is pure Java");
    rule.check(importedClasses);
  }
}
