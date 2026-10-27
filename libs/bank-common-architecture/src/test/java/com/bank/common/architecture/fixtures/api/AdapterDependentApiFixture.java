package com.bank.common.architecture.fixtures.api;

import com.bank.common.architecture.fixtures.adapter.FixtureAdapter;

/** Violation fixture — API depending on an adapter. */
public final class AdapterDependentApiFixture {
  private final FixtureAdapter adapter = new FixtureAdapter();

  public String handle() {
    return adapter.ping();
  }
}
