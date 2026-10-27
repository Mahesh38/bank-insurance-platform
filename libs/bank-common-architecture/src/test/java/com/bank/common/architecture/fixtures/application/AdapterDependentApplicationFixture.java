package com.bank.common.architecture.fixtures.application;

import com.bank.common.architecture.fixtures.adapter.FixtureAdapter;

/** Violation fixture — application depending on an adapter. */
public final class AdapterDependentApplicationFixture {
  private final FixtureAdapter adapter = new FixtureAdapter();

  public String run() {
    return adapter.ping();
  }
}
