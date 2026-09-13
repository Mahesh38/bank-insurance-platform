package com.bank.common.test.e2e;

/**
 * S08-E03-S05 — standard JUnit tags for end-to-end harness tests.
 *
 * <p>E2E tests in this repository currently run in-process with WireMock at the 1SB and
 * persistence seams (no remote environment required). Tag them so CI can report the pyramid
 * level separately from unit and module ITs.
 */
public final class E2ETags {

    public static final String E2E = "e2e";
    public static final String ASSISTED_LIFE_SMOKE = "e2e:assisted-life-smoke";

    private E2ETags() {
    }
}
