package com.bank.common.events.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;

/** H2 database with the outbox and processed_event DDL documented in CATALOGUE.md. */
final class H2Outbox {

  private H2Outbox() {}

  static DataSource create() throws SQLException {
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000");
    try (Connection c = ds.getConnection();
        Statement s = c.createStatement()) {
      s.execute("CREATE SCHEMA evt");
      s.execute(
          "CREATE TABLE evt.outbox_event ("
              + " outbox_id CHAR(26) PRIMARY KEY,"
              + " aggregate_type VARCHAR(64) NOT NULL,"
              + " aggregate_id VARCHAR(64) NOT NULL,"
              + " event_type VARCHAR(96) NOT NULL,"
              + " payload VARCHAR(16000) NOT NULL,"
              + " created_at TIMESTAMP WITH TIME ZONE NOT NULL,"
              + " published_at TIMESTAMP WITH TIME ZONE,"
              + " attempts INTEGER DEFAULT 0 NOT NULL,"
              + " last_error VARCHAR(1000),"
              + " parked_at TIMESTAMP WITH TIME ZONE)");
      s.execute(
          "CREATE TABLE evt.processed_event ("
              + " event_id CHAR(26) NOT NULL,"
              + " consumer VARCHAR(96) NOT NULL,"
              + " processed_at TIMESTAMP WITH TIME ZONE NOT NULL,"
              + " PRIMARY KEY (event_id, consumer))");
    }
    return ds;
  }

  static int count(DataSource ds, String where) throws SQLException {
    try (Connection c = ds.getConnection();
        Statement s = c.createStatement();
        var rs = s.executeQuery("SELECT COUNT(*) FROM evt.outbox_event WHERE " + where)) {
      rs.next();
      return rs.getInt(1);
    }
  }
}
