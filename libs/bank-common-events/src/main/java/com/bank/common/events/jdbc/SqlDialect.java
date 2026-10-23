package com.bank.common.events.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * The SQL differences the outbox and the processed-event store care about. Detected from {@link
 * java.sql.DatabaseMetaData#getDatabaseProductName()} unless configured.
 *
 * <p>{@code SKIP LOCKED} is used only on PostgreSQL (Aurora), where it is what makes several relay
 * replicas safe and non-blocking (ADR-025 §3). Other databases (H2 in tests) claim with plain
 * {@code FOR UPDATE}: a second relay waits for the first to commit and then re-reads the rows, so
 * it never double-publishes but does not run in parallel either.
 */
public enum SqlDialect {
  POSTGRES("CAST(? AS jsonb)", " FOR UPDATE SKIP LOCKED", true),
  GENERIC("?", " FOR UPDATE", false);

  private final String payloadPlaceholder;
  private final String claimLock;
  private final boolean onConflictDoNothing;

  SqlDialect(String payloadPlaceholder, String claimLock, boolean onConflictDoNothing) {
    this.payloadPlaceholder = payloadPlaceholder;
    this.claimLock = claimLock;
    this.onConflictDoNothing = onConflictDoNothing;
  }

  /**
   * Placeholder for the payload column. On PostgreSQL the cast makes a JDBC string acceptable to a
   * {@code jsonb} column and still assigns to a {@code text}/{@code varchar} one.
   */
  String payloadPlaceholder() {
    return payloadPlaceholder;
  }

  String claimLock() {
    return claimLock;
  }

  boolean onConflictDoNothing() {
    return onConflictDoNothing;
  }

  public static SqlDialect detect(Connection connection) throws SQLException {
    String product = connection.getMetaData().getDatabaseProductName();
    return product != null && product.toLowerCase(Locale.ROOT).contains("postgres")
        ? POSTGRES
        : GENERIC;
  }

  /** Schema names are interpolated into SQL, so only plain identifiers are accepted. */
  static String requireSchema(String schema) {
    if (schema == null || !schema.matches("^[a-z_][a-z0-9_]{0,62}$")) {
      throw new IllegalArgumentException("schema must be a plain lower-case identifier: " + schema);
    }
    return schema;
  }
}
