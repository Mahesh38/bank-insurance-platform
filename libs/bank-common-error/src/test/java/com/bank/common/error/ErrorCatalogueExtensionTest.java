package com.bank.common.error;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/** The extension point, and the one thing it must refuse. */
class ErrorCatalogueExtensionTest {

    @Test
    void aProviderContributesItsOwnCodes() {
        ErrorDefinitionProvider provider = () -> List.of(new ErrorDefinition(
            "SECOND_LOB_NOT_ENABLED", ErrorCategory.VALIDATION, 422, Retryability.NO,
            "Product line not enabled", "This product line is not available yet.",
            AuditDisposition.NONE, Propagation.PROPAGATE, null, "module"));

        assertThat(provider.definitions()).singleElement()
            .satisfies(d -> {
                assertThat(d.code()).isEqualTo("SECOND_LOB_NOT_ENABLED");
                assertThat(d.runbook())
                    .as("a contributed code carries a runbook like any other")
                    .isEqualTo("RB-SECOND_LOB_NOT_ENABLED");
            });
    }

    @Test
    void theContractForbidsRedefiningAPlatformCode() {
        // Documented here because the enforcement runs at class initialisation, which a test
        // cannot re-trigger: ErrorCatalogue.loadRegistry throws when a provider returns a code that
        // already exists. A module quietly changing SUITABILITY_REQUIRED's status or wording would
        // reintroduce D4 through the one door the registry exists to close, and would surface only
        // as two services answering differently in production.
        assertThat(ErrorCatalogue.isRegistered(ErrorCodes.SUITABILITY_REQUIRED)).isTrue();
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ErrorCatalogue.require("SECOND_LOB_NOT_ENABLED"))
            .withMessageContaining("Unregistered error code");
    }
}
