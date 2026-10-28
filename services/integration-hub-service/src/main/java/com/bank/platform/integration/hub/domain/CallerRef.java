package com.bank.platform.integration.hub.domain;

import java.util.Objects;

/** Who owns the provider request — used to refuse cross-caller polls. */
public record CallerRef(String context, String aggregateId) {

  public CallerRef {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(aggregateId, "aggregateId");
  }
}
