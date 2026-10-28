package com.bank.platform.integration.hub.domain;

/** Hub audit action names written to {@code hub.outbox_event}. */
public final class HubAuditActions {

  public static final String PROVIDER_REQUEST_DISPATCHED = "PROVIDER_REQUEST_DISPATCHED";
  public static final String PROVIDER_REQUEST_FAILED = "PROVIDER_REQUEST_FAILED";
  public static final String ATTRIBUTION_REFUSED = "ATTRIBUTION_NOT_CALLER_SUPPLIED";

  private HubAuditActions() {}
}
