package com.bank.common.events.jdbc;

/**
 * What one {@link JdbcOutboxRelay#relayOnce()} did: rows claimed, published, failed (will retry),
 * parked (stopped after {@code maxAttempts}; alert) and deferred (left for the next cycle because
 * an earlier event of the same aggregate failed in this batch).
 */
public record RelayResult(int claimed, int published, int failed, int parked, int deferred) {

  public static final RelayResult EMPTY = new RelayResult(0, 0, 0, 0, 0);
}
