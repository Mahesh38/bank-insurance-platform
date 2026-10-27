package com.bank.common.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Shared hexagonal baseline for every {@code services/*} module (EPIC-007 / ARCH-029).
 *
 * <p>Rules use {@code allowEmptyShould(true)} so H0 scaffolds with empty {@code application} /
 * {@code domain} / {@code api} packages stay green. Populated packages still fail on a real
 * layering breach. Service-specific rules (for example 1SB adapter isolation) stay in that
 * service's {@code ArchitectureTest}.
 *
 * <p>CODING-STANDARD.md section 5 — layers are {@code api}, {@code application}, {@code domain},
 * {@code adapter}, {@code config}.
 */
public final class HexagonalArchitecture {

  private HexagonalArchitecture() {}

  /** Application use-cases depend on ports, not adapter implementations. */
  public static ArchRule applicationDoesNotDependOnAdapters() {
    return noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapter..")
        .allowEmptyShould(true)
        .because("Application layer depends on domain ports, not adapters");
  }

  /** HTTP/API types talk to the application service, not adapters. */
  public static ArchRule apiDoesNotDependOnAdapters() {
    return noClasses()
        .that()
        .resideInAPackage("..api..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..adapter..")
        .allowEmptyShould(true)
        .because("API layer talks to the application service");
  }

  /** Domain stays free of Spring and of adapters. */
  public static ArchRule domainDoesNotDependOnSpringOrAdapters() {
    return noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "..adapter..")
        .allowEmptyShould(true)
        .because("Domain layer is pure Java");
  }

  /** Runs the three baseline rules against the imported production classes. */
  public static void checkBaseline(JavaClasses classes) {
    applicationDoesNotDependOnAdapters().check(classes);
    apiDoesNotDependOnAdapters().check(classes);
    domainDoesNotDependOnSpringOrAdapters().check(classes);
  }
}
