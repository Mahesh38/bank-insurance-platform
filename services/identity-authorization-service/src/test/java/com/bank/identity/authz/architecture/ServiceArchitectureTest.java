package com.bank.identity.authz.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.common.architecture.HexagonalArchitecture;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Hexagonal boundaries for authz. Uses the shared EPIC-007 / ARCH-029 baseline. */
@Tag("unit")
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
  void sharedHexagonalBaselineHolds() {
    HexagonalArchitecture.checkBaseline(importedClasses);
  }
}
