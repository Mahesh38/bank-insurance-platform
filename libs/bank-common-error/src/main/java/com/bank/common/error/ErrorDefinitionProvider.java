package com.bank.common.error;

import java.util.Collection;

/**
 * Contributes error definitions to {@link ErrorCatalogue} from outside this library.
 *
 * <p>The platform's own codes — everything seeded from
 * {@code docs/journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md} — are built in. This
 * interface exists so a module with refusals of its own (a second LOB, a future bounded context)
 * can register them without editing a shared library and forcing every consumer to take a new
 * version of it.
 *
 * <p>Discovered by {@link java.util.ServiceLoader}. Declare an implementation in
 * {@code META-INF/services/com.bank.common.error.ErrorDefinitionProvider}.
 *
 * <h2>What a provider may not do</h2>
 * <strong>Redefine a code that already exists.</strong> A module silently changing the status,
 * wording or retryability of {@code SUITABILITY_REQUIRED} would reintroduce defect D4 through the
 * one door the registry was built to close, and it would do so invisibly — the conflict would only
 * appear as two services answering differently in production. {@link ErrorCatalogue} fails fast on
 * a duplicate instead.
 *
 * <p>A provider is therefore additive only, and its codes are subject to the same rules as the
 * platform's: a fixed, safe {@code publicTitle} and {@code publicDetail}, and a runbook page.
 */
@FunctionalInterface
public interface ErrorDefinitionProvider {

    /** The definitions this module contributes. Codes must not already exist in the catalogue. */
    Collection<ErrorDefinition> definitions();
}
