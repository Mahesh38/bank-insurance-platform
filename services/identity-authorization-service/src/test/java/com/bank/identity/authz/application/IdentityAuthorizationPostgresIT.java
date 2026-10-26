package com.bank.identity.authz.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bank.common.test.PyramidTags;
import com.bank.common.test.postgres.PostgresTestSupport;
import com.bank.identity.authz.application.port.IdentityProvisioningPort;
import com.bank.identity.authz.persistence.AuthorizationFactsRepository;
import com.bank.identity.authz.persistence.BusinessUserEntity;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import com.bank.identity.authz.support.AuthzFixtures;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The H2 suite cannot see PostgreSQL-only failures: Flyway needs {@code flyway-database-postgresql}
 * to migrate, and the PG JDBC driver refuses a raw {@link Instant} bind parameter. This boots the
 * service on real PostgreSQL and drives the outbox claim, dead-letter and policy-facts queries.
 */
@SpringBootTest(
    properties = {
      "identity.provisioning.enabled=true",
      "identity.provisioning.initial-delay=3600000",
      "identity.provisioning.poll-interval=3600000",
      "identity.provisioning.max-attempts=1"
    })
@Testcontainers(disabledWithoutDocker = true)
@Tag(PyramidTags.INTEGRATION)
@Tag(PyramidTags.TESTCONTAINERS)
class IdentityAuthorizationPostgresIT {

  @Container
  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> POSTGRES = PostgresTestSupport.create();

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    PostgresTestSupport.register(POSTGRES, registry);
  }

  @Autowired ProvisioningOutboxDispatcher dispatcher;
  @Autowired AuthorizationFactsRepository facts;
  @Autowired BusinessUserRepository users;
  @Autowired JdbcClient jdbc;
  @MockitoBean IdentityProvisioningPort provider;

  UUID userId;
  UUID eventId;

  @BeforeEach
  void pendingPartnerWithOutboxRow() {
    jdbc.sql("delete from outbox_event").update();
    if (jdbc.sql("select count(*) from insurer where code = 'ICICI_PRU'").query(Long.class).single()
        == 0) {
      AuthzFixtures.insurer(jdbc, "ICICI_PRU");
    }
    userId =
        users
            .save(
                BusinessUserEntity.pendingPartner(
                    "partner." + UUID.randomUUID(), "p@example.test", "P", "Q", "ICICI_PRU"))
            .getId();
    eventId = UUID.randomUUID();
    jdbc.sql(
            """
            insert into outbox_event
                (id, aggregate_type, aggregate_id, event_type, payload, created_at)
            values (:id, 'BUSINESS_USER', :userId, 'IDENTITY_PROVISIONING_REQUESTED', '{}', :now)
            """)
        .param("id", eventId)
        .param("userId", userId)
        .param("now", Timestamp.from(Instant.now()))
        .update();
  }

  @Test
  void flywayMigratesAndClaimUsesSkipLocked() {
    assertThat(dispatcher.lockClause()).isEqualTo(" for update skip locked");
    assertThat(dispatcher.claim())
        .extracting(ProvisioningOutboxDispatcher.OutboxItem::eventId)
        .containsExactly(eventId);
    assertThat(dispatcher.claim()).isEmpty();
  }

  @Test
  void provisioningPublishesAndActivatesOnPostgres() {
    when(provider.provision(any())).thenReturn("provider-subject-pg");

    dispatcher.dispatch();

    assertThat(column("published_at")).isNotNull();
    assertThat(users.findById(userId).orElseThrow().getProviderSubject())
        .isEqualTo("provider-subject-pg");
  }

  @Test
  void failureIsDeadLetteredOnPostgres() {
    when(provider.provision(any())).thenThrow(new IllegalStateException("adapter down"));

    dispatcher.dispatch();

    assertThat(column("dead_lettered_at")).isNotNull();
  }

  @Test
  void policyFactsLoadOnPostgres() {
    assertThat(facts.load(userId, Instant.now())).isPresent();
  }

  private Object column(String name) {
    return jdbc.sql("select " + name + " from outbox_event where id = :id")
        .param("id", eventId)
        .query((rs, rowNum) -> java.util.Optional.ofNullable(rs.getObject(1)))
        .single()
        .orElse(null);
  }
}
