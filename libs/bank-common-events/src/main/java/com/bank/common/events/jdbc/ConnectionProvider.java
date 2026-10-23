package com.bank.common.events.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Supplies the connection of the caller's current transaction. The Spring implementation is {@code
 * com.bank.common.events.spring.SpringConnectionProvider}; plain JDBC callers pass the {@link
 * Connection} directly instead.
 */
public interface ConnectionProvider {

  Connection acquire() throws SQLException;

  void release(Connection connection);
}
