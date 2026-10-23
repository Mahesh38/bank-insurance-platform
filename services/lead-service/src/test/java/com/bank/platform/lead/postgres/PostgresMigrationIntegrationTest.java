package com.bank.platform.lead.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.platform.lead.adapter.persistence.JdbcLeadRepository;
import com.bank.platform.lead.domain.ConcurrentLeadUpdateException;
import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.ProductClass;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves the PostgreSQL-only SQL on a real server (READINESS-REVIEW §3.5; H2 cannot): Flyway V1–V7
 * including the vendor migrations, the partial unique {@code ux_lead_dedupe_open}, the append-only
 * triggers on the exception evidence, and {@code FOR UPDATE SKIP LOCKED} in the expiry claim.
 *
 * <p>Runs only with {@code -Dpg.it=true}; creates its own database on {@code pg.url} (default the
 * local trust-auth instance {@code localhost:55432}, user {@code postgres}) and never drops others.
 */
@EnabledIfSystemProperty(named = "pg.it", matches = "true")
@SpringBootTest(properties = {"lead.expiry.enabled=false"})
class PostgresMigrationIntegrationTest {

  private static final String SERVER =
      System.getProperty("pg.url", "jdbc:postgresql://localhost:55432/");
  private static final String DATABASE =
      "lead_it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  private static final Instant T0 = Instant.parse("2026-11-15T06:00:00Z");

  @DynamicPropertySource
  static void freshDatabase(DynamicPropertyRegistry registry) throws SQLException {
    try (Connection c = DriverManager.getConnection(SERVER + "postgres", "postgres", "");
        Statement s = c.createStatement()) {
      s.execute("CREATE DATABASE " + DATABASE.toLowerCase(Locale.ROOT));
    }
    registry.add("spring.datasource.url", () -> SERVER + DATABASE);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.datasource.username", () -> "postgres");
    registry.add("spring.datasource.password", () -> "");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired JdbcLeadRepository leads;
  @Autowired TransactionTemplate tx;

  @Test
  void everyMigrationIncludingTheVendorOnesApplies() {
    List<String> applied =
        jdbc.queryForList(
            "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
            String.class);
    assertThat(applied).containsExactly("1", "2", "3", "4", "5", "6", "7");
    assertThat(
            jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname = 'ux_lead_dedupe_open'",
                String.class))
        .contains("UNIQUE")
        .contains("WHERE");
  }

  @Test
  void partialUniqueIndexAllowsOneOpenLeadPerKey() {
    Lead first = lead("01JQX4K7R8M2N3P4Q5S6T7VA01", "rm-pg-1", "cust-pg-1");
    leads.save(first);
    assertThatThrownBy(() -> leads.save(lead("01JQX4K7R8M2N3P4Q5S6T7VA02", "rm-pg-1", "cust-pg-1")))
        .isInstanceOf(ConcurrentLeadUpdateException.class);

    first.close(T0.plusSeconds(1));
    leads.save(first);
    leads.save(lead("01JQX4K7R8M2N3P4Q5S6T7VA03", "rm-pg-1", "cust-pg-1"));
  }

  @Test
  void exceptionEvidenceIsAppendOnly() {
    leads.save(lead("01JQX4K7R8M2N3P4Q5S6T7VB01", "rm-pg-2", "cust-pg-2"));
    jdbc.update(
        "INSERT INTO lead_lms.lead_exception_evaluation (evaluation_id, lead_id, checkpoint,"
            + " rule_set_id, rule_set_version, engine_version, inputs_hash, outcome, reason_codes,"
            + " fired_rules, messages, evaluated_by, evaluated_at) VALUES ("
            + "'01JQX4K7R8M2N3P4Q5S6T7VB02', '01JQX4K7R8M2N3P4Q5S6T7VB01', 'START_ONBOARDING',"
            + " 'LEAD.EXCEPTION', '1.1.0', '1.0.0', 'sha256:x', 'BLOCK', '[]', '[]', '[]', 'rm',"
            + " now())");

    assertThatThrownBy(
            () -> jdbc.update("UPDATE lead_lms.lead_exception_evaluation SET outcome = 'PASS'"))
        .rootCause()
        .hasMessageContaining("append-only");
    assertThatThrownBy(() -> jdbc.update("DELETE FROM lead_lms.lead_exception_evaluation"))
        .rootCause()
        .hasMessageContaining("append-only");
  }

  @Test
  void overdueClaimSkipsRowsAnotherSweeperHolds() throws Exception {
    for (int i = 0; i < 3; i++) {
      Lead l = lead("01JQX4K7R8M2N3P4Q5S6T7VC0" + i, "rm-pg-3", "cust-pg-3-" + i);
      l.expireAt(T0.plus(1, ChronoUnit.DAYS));
      leads.save(l);
    }
    Instant now = T0.plus(2, ChronoUnit.DAYS);
    CountDownLatch claimed = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    CompletableFuture<Integer> first =
        CompletableFuture.supplyAsync(
            () ->
                tx.execute(
                    status -> {
                      int n = leads.lockOverdue(now, 2).size();
                      claimed.countDown();
                      await(release);
                      return n;
                    }));
    assertThat(claimed.await(10, TimeUnit.SECONDS)).isTrue();
    List<String> second =
        tx.execute(status -> leads.lockOverdue(now, 10).stream().map(Lead::leadId).toList());
    release.countDown();

    assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(2);
    assertThat(second).hasSize(1); // the two held rows are skipped, not waited for
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static Lead lead(String id, String creator, String customer) {
    return new Lead(id, customer, "LIFE", ProductClass.TERM, "BR-1", creator, T0);
  }
}
