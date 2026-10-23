package com.bank.identity.provider.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Hexagonal fitness tests for identity-provider-adapter-service (READINESS-REVIEW §3.7: this
 * service had none). Pattern copied from lead-service's {@code ServiceArchitectureTest}.
 *
 * <p>The service has no {@code application} package — the controller calls the provider-neutral
 * domain ports directly — so the lead rule "application must not depend on adapters" is expressed
 * here as "api must not depend on adapters". Every package is populated, so {@code
 * allowEmptyShould} is not used: an empty selector would be a failure, not a pass.
 */
class ServiceArchitectureTest {

  private static final String BASE_PACKAGE = "com.bank.identity.provider";

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
  void domainMustNotDependOnSpring() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework..")
        .as("Domain ports are pure Java")
        .check(importedClasses);
  }

  @Test
  void domainMustNotDependOnOuterLayers() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..adapter..", "..api..", "..config..")
        .as("Domain does not know its adapters, API or wiring")
        .check(importedClasses);
  }

  @Test
  void apiMustNotDependOnAdapters() {
    noClasses()
        .that()
        .resideInAPackage("..api..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapter..")
        .as("Controllers call domain ports, never a provider adapter directly")
        .check(importedClasses);
  }

  @Test
  void adaptersMustNotDependOnApi() {
    noClasses()
        .that()
        .resideInAPackage("..adapter..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..api..")
        .as("Adapters do not reach back into the HTTP layer")
        .check(importedClasses);
  }
}
