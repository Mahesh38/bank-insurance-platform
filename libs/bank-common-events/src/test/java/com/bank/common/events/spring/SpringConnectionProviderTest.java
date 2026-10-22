package com.bank.common.events.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.events.EventJsonCodec;
import com.bank.common.events.TestEvents;
import com.bank.common.events.Ulid;
import com.bank.common.events.jdbc.OutboxWriter;
import java.util.UUID;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class SpringConnectionProviderTest {

  private DataSource ds;
  private JdbcTemplate jdbc;
  private TransactionTemplate tx;
  private OutboxWriter writer;

  @BeforeEach
  void setUp() {
    JdbcDataSource h2 = new JdbcDataSource();
    h2.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    ds = h2;
    jdbc = new JdbcTemplate(ds);
    jdbc.execute("CREATE SCHEMA svc");
    jdbc.execute("CREATE TABLE svc.business (id VARCHAR(26) PRIMARY KEY)");
    jdbc.execute(
        "CREATE TABLE svc.outbox_event (outbox_id CHAR(26) PRIMARY KEY, aggregate_type VARCHAR(64),"
            + " aggregate_id VARCHAR(64), event_type VARCHAR(96), payload VARCHAR(16000),"
            + " created_at TIMESTAMP WITH TIME ZONE, published_at TIMESTAMP WITH TIME ZONE,"
            + " attempts INTEGER DEFAULT 0, last_error VARCHAR(1000),"
            + " parked_at TIMESTAMP WITH TIME ZONE)");
    tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    writer =
        new OutboxWriter(
            "svc",
            new EventJsonCodec(),
            TestEvents.tickingClock(),
            new SpringConnectionProvider(ds),
            null);
  }

  @Test
  void outboxRowCommitsAndRollsBackWithTheBusinessChange() {
    tx.executeWithoutResult(
        s -> {
          jdbc.update("INSERT INTO svc.business VALUES (?)", "a");
          writer.write(TestEvents.event(Ulid.mint(), "PaymentCaptured"));
        });
    tx.executeWithoutResult(
        s -> {
          jdbc.update("INSERT INTO svc.business VALUES (?)", "b");
          writer.write(TestEvents.event(Ulid.mint(), "PaymentCaptured"));
          s.setRollbackOnly();
        });

    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM svc.business", Integer.class)).isOne();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM svc.outbox_event", Integer.class)).isOne();
  }

  @Test
  void refusesToWriteOutsideATransaction() {
    assertThatThrownBy(() -> writer.write(TestEvents.event(Ulid.mint(), "PaymentCaptured")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("business transaction");
  }
}
