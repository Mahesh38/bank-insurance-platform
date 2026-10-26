package com.bank.common.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.architecture.fixtures.adapter.FixtureAdapter;
import com.bank.common.architecture.fixtures.application.AdapterDependentApplicationFixture;
import com.bank.common.architecture.fixtures.domain.SpringDependentDomainFixture;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Proves the shared baseline rejects real layering breaches (EPIC-007 / ARCH-029), the same way
 * {@code ArchitectureRulesBiteTest} does for 1SB-specific rules.
 */
@Tag("unit")
class HexagonalArchitectureBiteTest {

  private static final String FIXTURE_PACKAGE = "com.bank.common.architecture.fixtures";

  private static JavaClasses fixtureClasses;

  @BeforeAll
  static void importFixtures() {
    fixtureClasses = new ClassFileImporter().importPackages(FIXTURE_PACKAGE);
  }

  @Test
  void fixturesArePresent() {
    assertThat(fixtureClasses).isNotEmpty();
    assertThat(fixtureClasses.stream().map(c -> c.getName()))
        .contains(
            FixtureAdapter.class.getName(),
            AdapterDependentApplicationFixture.class.getName(),
            SpringDependentDomainFixture.class.getName());
  }

  @Test
  void applicationDependingOnAdapterIsRejected() {
    assertThatThrownBy(
            () -> HexagonalArchitecture.applicationDoesNotDependOnAdapters().check(fixtureClasses))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("AdapterDependentApplicationFixture");
  }

  @Test
  void domainDependingOnSpringIsRejected() {
    assertThatThrownBy(
            () ->
                HexagonalArchitecture.domainDoesNotDependOnSpringOrAdapters().check(fixtureClasses))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("SpringDependentDomainFixture");
  }

  @Test
  void apiDependingOnAdapterIsRejected() {
    assertThatThrownBy(
            () -> HexagonalArchitecture.apiDoesNotDependOnAdapters().check(fixtureClasses))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("AdapterDependentApiFixture");
  }
}
