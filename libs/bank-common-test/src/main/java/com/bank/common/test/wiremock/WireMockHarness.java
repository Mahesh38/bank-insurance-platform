package com.bank.common.test.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * S08-E03-S02 / TD-014 — shared dual WireMock harness for 1SB and bank-persistence seams.
 *
 * <p>Typical use in a service IT:
 *
 * <pre>{@code
 * static final WireMockHarness HARNESS = WireMockHarness.start();
 * @AfterAll static void stop() { HARNESS.close(); }
 * @DynamicPropertySource
 * static void props(DynamicPropertyRegistry r) { HARNESS.register(r); }
 * }</pre>
 */
public final class WireMockHarness implements AutoCloseable {

  private final WireMockServer onesb;
  private final WireMockServer persistence;

  private WireMockHarness(WireMockServer onesb, WireMockServer persistence) {
    this.onesb = onesb;
    this.persistence = persistence;
  }

  public static WireMockHarness start() {
    WireMockServer onesb = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    WireMockServer persistence =
        new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    onesb.start();
    persistence.start();
    return new WireMockHarness(onesb, persistence);
  }

  public WireMockServer onesb() {
    return onesb;
  }

  public WireMockServer persistence() {
    return persistence;
  }

  public void reset() {
    onesb.resetAll();
    persistence.resetAll();
  }

  /** Binds the property names used by {@code 1sb-integration-service} today. */
  public void register(org.springframework.test.context.DynamicPropertyRegistry registry) {
    registry.add("onesb.client.base-url", onesb::baseUrl);
    registry.add("bank.persistence.base-url", persistence::baseUrl);
  }

  @Override
  public void close() {
    onesb.stop();
    persistence.stop();
  }
}
