package com.bank.workforce.bff.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Layering fitness tests for workforce-access-bff (READINESS-REVIEW §3.7: this service had none).
 * Pattern from lead-service's {@code ServiceArchitectureTest}, adapted to the BFF's layout.
 *
 * <p>Fit to the existing layout: the BFF has no {@code domain} and no {@code adapter} package — it
 * is an edge orchestrator. Its outbound side is {@code client} (identity, PDP), {@code lead} and
 * {@code customer} (gateway port + HTTP and stub implementations side by side) and {@code session}
 * (vault). So the lead rules "domain must not import Spring" and "application must not depend on
 * adapter" have no direct equivalent; the rules below are the ones that do fit: the HTTP layer is
 * on top, nothing outbound reaches back up, and callers depend on the gateway interfaces, never on
 * the HTTP or stub implementation chosen by {@code *-mode}.
 */
class ServiceArchitectureTest {

  private static final String BASE_PACKAGE = "com.bank.workforce.bff";
  private static final String[] OUTBOUND = {
    "..client..", "..lead..", "..customer..", "..session.."
  };

  private static final String MODE_SELECTED_IMPLEMENTATIONS =
      "com\\.bank\\.workforce\\.bff\\.(lead|customer|session)\\.(Http|Stub|InMemory|Redis).*";

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
  void applicationMustNotDependOnApi() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..api..")
        .as("Application services do not know the HTTP layer")
        .check(importedClasses);
  }

  @Test
  void outboundSideMustNotReachBackUp() {
    noClasses()
        .that()
        .resideInAnyPackage(OUTBOUND)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..api..", "..application..")
        .as("Clients, gateways and the session vault do not call back into api or application")
        .check(importedClasses);
  }

  @Test
  void callersUseGatewayInterfacesNotImplementations() {
    noClasses()
        .that()
        .resideInAnyPackage("..api..", "..application..")
        .should()
        .dependOnClassesThat()
        .haveNameMatching(MODE_SELECTED_IMPLEMENTATIONS)
        .as("Mode-selected implementations are reached only through their interface")
        .check(importedClasses);
  }
}
