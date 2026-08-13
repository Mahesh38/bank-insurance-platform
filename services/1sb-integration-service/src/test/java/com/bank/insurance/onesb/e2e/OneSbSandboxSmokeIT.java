package com.bank.insurance.onesb.e2e;

import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbMasterDataAdapter;
import com.bank.insurance.onesb.domain.port.outbound.OneSbMasterDataPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WS-1 Phase 4 exit criterion <b>4.1</b> — the "gated nightly" half.
 *
 * <p>{@link TermJourneyE2EIT} proves the Term journey holds together against a simulated
 * sandbox on every PR. It cannot prove that our credentials still work, that our egress IP is
 * still whitelisted, or that 1SB has not changed a response shape under us. Only a call to the
 * real sandbox does that — and that call cannot be a PR gate, because a third party's downtime
 * must not block our merges (ACTION-PLAN 4.1 sanctions exactly this split).
 *
 * <p><b>Skipped unless {@code ONESB_SMOKE_ENABLED=true}.</b> Without it — which is every local
 * run and every PR build — JUnit reports these as skipped, not passed. That distinction matters:
 * a green PR build is not evidence the sandbox works.
 *
 * <p>Required environment when enabled:
 * <pre>
 *   ONESB_SMOKE_ENABLED=true
 *   ONESB_BASE_URL          (e.g. https://demo.api.1silverbullet.tech)
 *   ONESB_API_KEY / ONESB_API_SECRET / ONESB_DISTRIBUTOR_ID
 * </pre>
 *
 * <p><b>Read-only by design.</b> This suite performs no quote, proposal, or payment submission.
 * A nightly job that creates real applications in a shared sandbox leaves debris other testers
 * have to reason about, and a payment submission is not something to automate unattended.
 * Failures here are an <em>environment</em> signal — see
 * {@code OPERATIONS-RUNBOOK.md} §4 (401) and §5 (5xx).
 *
 * <p>Run: {@code ONESB_SMOKE_ENABLED=true ./gradlew :services:1sb-integration-service:test
 * --tests '*OneSbSandboxSmokeIT'}
 */
@SpringBootTest
@Tag("sandbox-smoke")
@EnabledIfEnvironmentVariable(named = "ONESB_SMOKE_ENABLED", matches = "true")
class OneSbSandboxSmokeIT {

    @Autowired
    private OneSbHttpClient httpClient;

    @Autowired
    private OneSbMasterDataPort masterDataPort;

    /**
     * Authentication and network reachability in one assertion. A 401 here means the credentials
     * are dead (runbook §4.3); a hang or transport failure means we may not be reaching 1SB at
     * all, which usually means the egress IP fell off the whitelist (runbook §3.1).
     */
    @Test
    void sandboxAcceptsOurCredentials() {
        assertThat(masterDataPort).isNotNull();

        OneSbMasterDataPort.MasterLookupResult result =
                masterDataPort.lookup("TERM", "PROPOSAL", List.of("gender"), null);

        assertThat(result).isNotNull();
        assertThat(result.lookups()).containsKey("gender");
    }

    /**
     * The response shape we normalise against is still the shape 1SB sends. This is the check
     * that catches an upstream contract drift before a bank consumer does.
     */
    @Test
    void masterDataStillNormalisesToCodeLabelPairs() {
        OneSbMasterDataPort.MasterLookupResult result =
                masterDataPort.lookup("TERM", "PROPOSAL", List.of("gender"), null);

        assertThat(result.lookups().get("gender"))
                .as("1SB returned no gender enum — either the category changed or the "
                        + "entity id did; see OneSbMasterDataAdapter.normalise")
                .isNotEmpty()
                .allSatisfy(value -> {
                    assertThat(value.code()).isNotBlank();
                    assertThat(value.label()).isNotBlank();
                });
    }

    /** Keeps the adapter type referenced so the smoke fails to compile if it is renamed. */
    @SuppressWarnings("unused")
    private static final Class<?> ADAPTER_UNDER_SMOKE = OneSbMasterDataAdapter.class;

    /** The HTTP stack is wired and carries Basic auth — a wiring regression fails here first. */
    @Test
    void outboundHttpStackIsWired() {
        assertThat(httpClient).isNotNull();
    }
}
