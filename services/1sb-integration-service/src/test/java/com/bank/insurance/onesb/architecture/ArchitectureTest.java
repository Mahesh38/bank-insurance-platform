package com.bank.insurance.onesb.architecture;

import com.bank.common.secrets.SecretProvider;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ArchUnit architecture fitness tests enforcing the hexagonal architecture boundaries
 * defined in the 1SB Integration Service architecture document (section 3) and the
 * WS-3 standing constraints in
 * {@code docs/platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md} section 6.
 * <p>
 * <b>TD-007 closed (S08-E02-S02).</b> {@code allowEmptyShould(true)} has been removed from
 * every rule. It was added in Phase 1 so that empty scaffold packages would not fail the
 * build; every package these rules select is now populated, and leaving the flag in place
 * meant a rule whose selector silently stopped matching would report green forever. A rule
 * that passes vacuously is worse than no rule, because it reports success.
 * <p>
 * {@link #everyRuleSelectorMatchesRealClasses()} is the guard that keeps that true: it
 * asserts each selector still matches classes, so a future package rename turns into a
 * failing test rather than a silently disabled rule.
 * <p>
 * That these rules actually reject violations — rather than merely being present — is
 * proven separately by {@link ArchitectureRulesBiteTest} (validation test S08-VT-03).
 */
class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.bank.insurance.onesb";

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    /**
     * Guard for TD-007. Without {@code allowEmptyShould(true)} a rule whose selector matches
     * nothing fails loudly — which is the behaviour we want. This test states the expectation
     * directly so the reason is visible rather than implied by an absent flag.
     */
    @Test
    void everyRuleSelectorMatchesRealClasses() {
        assertThat(importedClasses).isNotEmpty();
        assertThat(countInPackage(".domain.")).as("domain layer").isPositive();
        assertThat(countInPackage(".application.")).as("application layer").isPositive();
        assertThat(countInPackage(".api.")).as("api layer").isPositive();
        assertThat(countInPackage(".adapter.onesb.")).as("1SB adapter").isPositive();
        assertThat(countInPackage(".lob.")).as("LOB handlers").isPositive();
        assertThat(countInPackage(".config.")).as("config layer").isPositive();
    }

    private long countInPackage(String fragment) {
        return importedClasses.stream()
                .filter(c -> c.getPackageName().contains(fragment.substring(0, fragment.length() - 1)))
                .count();
    }

    /**
     * The application layer must not import from any adapter.
     * Use-case orchestrators depend on port interfaces only.
     */
    @Test
    void applicationLayerMustNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .as("Application layer must depend only on domain ports, not adapter implementations");

        rule.check(importedClasses);
    }

    /**
     * The API layer must not directly import adapter classes.
     * Controllers must go through the application layer.
     */
    @Test
    void apiLayerMustNotImportAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..api..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .as("API controllers must delegate to application services, not adapters directly");

        rule.check(importedClasses);
    }

    /**
     * The domain layer must not import Spring Framework annotations or classes.
     * Domain is pure Java — no Spring dependency.
     */
    @Test
    void domainMustNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..")
                .as("Domain layer must be pure Java with zero Spring dependencies");

        rule.check(importedClasses);
    }

    /**
     * <b>INV-ACL-01 — provider isolation, strengthened.</b>
     * <p>
     * Previously this rule guarded only {@code adapter.onesb.client}. The standing constraint
     * is broader: no provider type — client, wire model, mapper or error shape — may appear
     * outside its own adapter package. A canonical model is only provider-neutral if the
     * provider's <em>models</em> are confined too, not merely its HTTP client.
     * <p>
     * {@code ..config..} is excluded because it is the composition root: Spring configuration
     * must be able to instantiate adapter beans to wire them to ports. That is the one
     * legitimate inbound reference and it carries no provider vocabulary into the domain.
     * <p>
     * Verified: no class outside {@code adapter.onesb} currently imports it, so this
     * strengthening locks in a property the codebase already has.
     */
    @Test
    void onlyOneSbAdapterMayImportOneSbAdapterPackage() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..adapter.onesb..")
                .and().resideOutsideOfPackage("..config..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter.onesb..")
                .as("INV-ACL-01: 1SB provider types are confined to adapter.onesb.*; "
                        + "all other packages use domain ports (config is the composition root)");

        rule.check(importedClasses);
    }

    /**
     * The domain layer must not import from adapter packages.
     */
    @Test
    void domainMustNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .as("Domain is adapter-agnostic; adapters implement domain ports, not the reverse");

        rule.check(importedClasses);
    }

    /**
     * LOB handlers must not import the persistence HTTP adapter.
     * They interact via domain ports only (JobStorePort, etc.).
     */
    @Test
    void lobHandlersMustNotImportPersistenceOrSecretAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..lob..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter.persistence..")
                .as("LOB handlers interact via domain ports only; "
                        + "they must not directly import the persistence HTTP adapter");

        rule.check(importedClasses);
    }

    /**
     * <b>New — dependency direction.</b> Adapters implement ports; they must never reach back
     * up into the HTTP API layer. An adapter that imports a controller DTO has quietly made
     * the wire format of the bank-facing API part of the provider integration contract, which
     * is the coupling the ports-and-adapters shape exists to prevent.
     */
    @Test
    void adaptersMustNotDependOnTheApiLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..adapter..")
                .should().dependOnClassesThat()
                .resideInAPackage("..api..")
                .as("Adapters must not depend on the API layer; dependencies point inward to domain ports");

        rule.check(importedClasses);
    }

    /**
     * <b>New — domain purity beyond Spring.</b> The domain must not depend on the JSON
     * serialization library or on Jakarta annotations either. Jackson annotations in a domain
     * record are how a wire format starts dictating a business model, and it happens one
     * convenient {@code @JsonProperty} at a time.
     */
    @Test
    void domainMustNotDependOnSerializationOrJakarta() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.fasterxml.jackson..", "jakarta..")
                .as("Domain must not depend on Jackson or Jakarta; "
                        + "serialization and persistence concerns belong in adapters");

        rule.check(importedClasses);
    }

    /**
     * <b>New — supports control C5 / INV-LOG-01 (no PII in logs).</b>
     * <p>
     * PII masking is applied by the logging framework's converter. Anything written straight
     * to {@code System.out} or {@code System.err} bypasses every appender, and therefore
     * bypasses masking entirely. This rule is the static half of the control; the runtime
     * half is the log-scan test for S08-G7.
     */
    @Test
    void mustNotWriteToStandardStreams() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should().accessField(System.class, "out")
                .orShould().accessField(System.class, "err")
                .as("C5/INV-LOG-01: all output goes through the logging framework so the "
                        + "PII masking converter applies; System.out/err bypasses masking");

        rule.check(importedClasses);
    }

    /**
     * SecretProvider implementations live only in :libs:bank-common-secrets.
     * The integration service may inject/wire SecretProvider but must not implement it.
     */
    @Test
    void mustNotImplementSecretProvider() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should().implement(SecretProvider.class)
                .as("SecretProvider implementations belong in bank-common-secrets, not the integration service");

        rule.check(importedClasses);
    }

    /**
     * adapter.secret package was removed (TD-002); providers live in the shared lib.
     */
    @Test
    void mustNotResideInAdapterSecretPackage() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should().resideInAPackage("..adapter.secret..")
                .as("No classes may reside in adapter.secret; use com.bank.common.secrets");

        rule.check(importedClasses);
    }

    /**
     * Config classes must not be imported by domain or application layer.
     * Config wires things together; domain and application must not depend on it.
     */
    @Test
    void configMustNotBeImportedByDomainOrApplication() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .or().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..config..")
                .as("Domain and application layers must not depend on Spring configuration classes");

        rule.check(importedClasses);
    }

    /**
     * After TD-003/011 split, integration service must not use JPA.
     * Persistence lives in bank-persistence-service; this service uses HTTP only.
     * Standing constraint: "1sb-integration-service owns no Flyway migrations and no JPA".
     */
    @Test
    void mustNotImportJakartaPersistence() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should().dependOnClassesThat()
                .resideInAPackage("jakarta.persistence..")
                .as("Integration service must not import jakarta.persistence (use HTTP persistence adapter)");

        rule.check(importedClasses);
    }

    /**
     * Flyway migrations and API must not appear in the integration service.
     */
    @Test
    void mustNotImportFlyway() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + "..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.flywaydb..")
                .as("Integration service must not import Flyway (owned by persistence service)");

        rule.check(importedClasses);
    }
}
