package com.bank.persistence.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.common.architecture.HexagonalArchitecture;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Hexagonal boundaries for bank-persistence. Uses the shared EPIC-007 / ARCH-029 baseline.
 *
 * <p>This module is a thin internal persistence facade: controllers currently inject Spring Data
 * repositories in {@code adapter.persistence} directly. The shared {@code api ↛ adapter} rule
 * therefore does not apply until an application layer is introduced (out of scope for EPIC-007).
 * Application and domain baseline rules still run.
 */
@Tag("unit")
class ServiceArchitectureTest {

  private static final String BASE_PACKAGE = "com.bank.persistence";

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
    HexagonalArchitecture.applicationDoesNotDependOnAdapters().check(importedClasses);
    HexagonalArchitecture.domainDoesNotDependOnSpringOrAdapters().check(importedClasses);
  }
}
