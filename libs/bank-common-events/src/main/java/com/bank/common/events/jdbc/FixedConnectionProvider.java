package com.bank.common.events.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Hands out one fixed connection — for plain JDBC callers that manage the transaction themselves
 * and want the {@link ConnectionProvider} overloads. Release is a no-op; the caller owns the
 * connection.
 */
public final class FixedConnectionProvider implements ConnectionProvider {

  private final Connection connection;

  public FixedConnectionProvider(Connection connection) {
    this.connection = Objects.requireNonNull(connection);
  }

  @Override
  public Connection acquire() throws SQLException {
    return connection;
  }

  @Override
  public void release(Connection c) {
    // The caller owns the connection and its transaction.
  }
}
