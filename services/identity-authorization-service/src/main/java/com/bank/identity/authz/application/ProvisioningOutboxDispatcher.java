package com.bank.identity.authz.application;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Drains {@code IDENTITY_PROVISIONING_REQUESTED} outbox rows into the identity provider.
 *
 * <p>READINESS-REVIEW §3.7 ("identity outbox dispatcher can double-provision"): with more than one
 * pod every instance read the same unpublished rows. Rows are now <em>claimed</em> in a short
 * transaction — {@code SELECT … FOR UPDATE SKIP LOCKED} on PostgreSQL — and leased via {@code
 * claimed_until}, so the slow provider call runs outside any lock and no other instance picks the
 * row up until the lease lapses. H2 (local / test only, one instance) does not need {@code SKIP
 * LOCKED} and gets a plain {@code FOR UPDATE}.
 *
 * <p>A row that fails {@code max-attempts} times is parked ({@code dead_lettered_at}) and logged at
 * ERROR for an operator, instead of being silently skipped forever.
 */
@Component
@ConditionalOnProperty(name = "identity.provisioning.enabled", havingValue = "true")
public class ProvisioningOutboxDispatcher {

  private static final Logger log = LoggerFactory.getLogger(ProvisioningOutboxDispatcher.class);

  private final JdbcClient jdbc;
  private final ProvisioningOutboxProcessor processor;
  private final TransactionTemplate transactions;
  private final DataSource dataSource;
  private final int batchSize;
  private final int maxAttempts;
  private final Duration lease;
  private volatile String lockClause;

  public ProvisioningOutboxDispatcher(
      JdbcClient jdbc,
      ProvisioningOutboxProcessor processor,
      TransactionTemplate transactions,
      DataSource dataSource,
      @Value("${identity.provisioning.batch-size:20}") int batchSize,
      @Value("${identity.provisioning.max-attempts:10}") int maxAttempts,
      @Value("${identity.provisioning.lease:2m}") Duration lease) {
    this.jdbc = jdbc;
    this.processor = processor;
    this.transactions = transactions;
    this.dataSource = dataSource;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
    this.lease = lease;
  }

  @Scheduled(
      fixedDelayString = "${identity.provisioning.poll-interval:5000}",
      initialDelayString = "${identity.provisioning.initial-delay:5000}")
  public void dispatch() {
    claim().forEach(this::dispatchSafely);
  }

  /** Claims up to {@code batch-size} due rows and leases them to this instance. */
  List<OutboxItem> claim() {
    Instant now = Instant.now();
    List<OutboxItem> claimed =
        transactions.execute(
            status -> {
              List<OutboxItem> items =
                  jdbc.sql(
                          """
                          select id, aggregate_id
                            from outbox_event
                           where published_at is null
                             and dead_lettered_at is null
                             and event_type = 'IDENTITY_PROVISIONING_REQUESTED'
                             and (claimed_until is null or claimed_until < :now)
                           order by created_at
                           fetch first :limit rows only
                          """
                              + lockClause())
                      .param("now", Timestamp.from(now))
                      .param("limit", batchSize)
                      .query(
                          (rs, rowNum) ->
                              new OutboxItem(
                                  rs.getObject("id", UUID.class),
                                  rs.getObject("aggregate_id", UUID.class)))
                      .list();
              if (!items.isEmpty()) {
                jdbc.sql("update outbox_event set claimed_until = :until where id in (:ids)")
                    .param("until", Timestamp.from(now.plus(lease)))
                    .param("ids", items.stream().map(OutboxItem::eventId).toList())
                    .update();
              }
              return items;
            });
    return claimed == null ? List.of() : claimed;
  }

  private void dispatchSafely(OutboxItem item) {
    try {
      processor.provision(item.eventId(), item.userId());
    } catch (RuntimeException exception) {
      recordFailure(item, exception.getClass().getSimpleName());
    }
  }

  private void recordFailure(OutboxItem item, String safeError) {
    jdbc.sql(
            """
            update outbox_event
               set attempts = attempts + 1, last_error = :error, claimed_until = null
             where id = :id and published_at is null
            """)
        .param("error", safeError)
        .param("id", item.eventId())
        .update();
    int attempts =
        jdbc.sql("select attempts from outbox_event where id = :id")
            .param("id", item.eventId())
            .query(Integer.class)
            .optional()
            .orElse(0);
    if (attempts >= maxAttempts) {
      jdbc.sql("update outbox_event set dead_lettered_at = :now where id = :id")
          .param("now", Timestamp.from(Instant.now()))
          .param("id", item.eventId())
          .update();
      log.error(
          "Partner identity provisioning dead-lettered after {} attempts; eventId={}, userId={},"
              + " error={}. Parked for operator action (dead_lettered_at set).",
          attempts,
          item.eventId(),
          item.userId(),
          safeError);
      return;
    }
    log.warn(
        "Partner identity provisioning attempt {} of {} failed; eventId={}, error={}",
        attempts,
        maxAttempts,
        item.eventId(),
        safeError);
  }

  /** {@code SKIP LOCKED} where the database supports it; plain row lock otherwise (H2). */
  String lockClause() {
    String clause = lockClause;
    if (clause == null) {
      clause = isPostgres() ? " for update skip locked" : " for update";
      lockClause = clause;
    }
    return clause;
  }

  private boolean isPostgres() {
    try (Connection connection = dataSource.getConnection()) {
      return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
    } catch (SQLException exception) {
      throw new IllegalStateException("Cannot read the datasource dialect", exception);
    }
  }

  record OutboxItem(UUID eventId, UUID userId) {}
}
