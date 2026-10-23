package com.bank.common.events.spring;

import com.bank.common.events.jdbc.ConnectionProvider;
import java.sql.Connection;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Joins the caller's Spring-managed transaction ({@code @Transactional}), so an {@code
 * OutboxWriter} insert or a {@code processed_event} claim commits with the business change. Refuses
 * to run outside a transaction: an outbox row written in its own auto-commit would reintroduce the
 * dual-write the outbox exists to remove (ADR-025 §3).
 */
public final class SpringConnectionProvider implements ConnectionProvider {

  private final DataSource dataSource;

  public SpringConnectionProvider(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource);
  }

  @Override
  public Connection acquire() {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException(
          "outbox and processed_event writes must run inside the business transaction");
    }
    return DataSourceUtils.getConnection(dataSource);
  }

  @Override
  public void release(Connection connection) {
    DataSourceUtils.releaseConnection(connection, dataSource);
  }
}
