package com.bank.identity.authz.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Hexagonal fitness tests for identity-authorization-service (READINESS-REVIEW §3.7: this service
 * had none). Pattern copied from lead-service's {@code ServiceArchitectureTest}; every selected
 * package has classes, so {@code allowEmptyShould} is not used.
 *
 * <p>Fit to the existing layout: persistence lives in {@code ..persistence..} (JPA entities and the
 * facts repository), not {@code ..adapter..}. The application services use it directly, so a
 * "application/api must not depend on persistence" rule would not hold without a repository-port
 * refactor (the controller maps {@code BusinessUserEntity} to its response); it is deliberately not
 * asserted here. Outbound HTTP is behind {@code application.port.IdentityProvisioningPort}, so
 * "application must not depend on adapters" holds.
 */
class ServiceArchitectureTest {

  private static final String BASE_PACKAGE = "com.bank.identity.authz";

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
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapter..")
        .as("Application layer depends on ports, not adapters")
        .check(importedClasses);
  }

  @Test
  void domainMustNotDependOnSpring() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.springframework..")
        .as("Domain layer is pure Java")
        .check(importedClasses);
  }

  @Test
  void domainMustNotDependOnOuterLayers() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..application..", "..adapter..", "..api..", "..config..", "..persistence..")
        .as("The PDP evaluator and its models know nothing outside the domain")
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
        .as("Controllers never call an outbound adapter directly")
        .check(importedClasses);
  }
}
