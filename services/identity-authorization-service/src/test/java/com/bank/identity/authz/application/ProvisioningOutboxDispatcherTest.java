package com.bank.identity.authz.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.identity.authz.application.port.IdentityProvisioningPort;
import com.bank.identity.authz.persistence.BusinessUserEntity;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import com.bank.identity.authz.support.AuthzFixtures;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** READINESS-REVIEW §3.7: claim with a lease, dead-letter after N attempts. */
@SpringBootTest(
    properties = {
      "identity.provisioning.enabled=true",
      "identity.provisioning.initial-delay=3600000",
      "identity.provisioning.poll-interval=3600000",
      "identity.provisioning.max-attempts=3"
    })
@Tag("integration")
class ProvisioningOutboxDispatcherTest {

  @Autowired ProvisioningOutboxDispatcher dispatcher;
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
    BusinessUserEntity user =
        users.save(
            BusinessUserEntity.pendingPartner(
                "partner." + UUID.randomUUID(), "p@example.test", "P", "Q", "ICICI_PRU"));
    userId = user.getId();
    eventId = UUID.randomUUID();
    jdbc.sql(
            """
            insert into outbox_event
                (id, aggregate_type, aggregate_id, event_type, payload, created_at)
            values (:id, 'BUSINESS_USER', :userId, 'IDENTITY_PROVISIONING_REQUESTED', '{}', :now)
            """)
        .param("id", eventId)
        .param("userId", userId)
        .param("now", Instant.now())
        .update();
  }

  @AfterEach
  void clean() {
    reset(provider);
    jdbc.sql("delete from outbox_event").update();
  }

  @Test
  void h2UsesPlainRowLockAndPostgresWouldSkipLocked() {
    assertThat(dispatcher.lockClause()).isEqualTo(" for update");
  }

  @Test
  void claimedRowIsLeasedSoASecondInstanceDoesNotTakeIt() {
    assertThat(dispatcher.claim())
        .extracting(ProvisioningOutboxDispatcher.OutboxItem::eventId)
        .containsExactly(eventId);
    assertThat(dispatcher.claim()).isEmpty();
  }

  @Test
  void successfulProvisioningPublishesAndActivates() {
    when(provider.provision(any())).thenReturn("provider-subject-1");

    dispatcher.dispatch();

    assertThat(publishedAt()).isNotNull();
    var user = users.findById(userId).orElseThrow();
    assertThat(user.getStatus().name()).isEqualTo("ACTIVE");
    assertThat(user.getProviderSubject()).isEqualTo("provider-subject-1");
  }

  @Test
  void repeatedFailureIsDeadLetteredAndNoLongerRetried() {
    when(provider.provision(any())).thenThrow(new IllegalStateException("adapter down"));

    dispatcher.dispatch();
    dispatcher.dispatch();
    assertThat(deadLetteredAt()).isNull();
    dispatcher.dispatch();

    assertThat(deadLetteredAt()).isNotNull();
    assertThat(publishedAt()).isNull();
    verify(provider, times(3)).provision(any());

    reset(provider);
    dispatcher.dispatch();
    verify(provider, never()).provision(any());
    assertThat(users.findById(userId).orElseThrow().getStatus().name()).isEqualTo("PENDING");
  }

  private Object publishedAt() {
    return column("published_at");
  }

  private Object deadLetteredAt() {
    return column("dead_lettered_at");
  }

  private Object column(String name) {
    return jdbc.sql("select " + name + " from outbox_event where id = :id")
        .param("id", eventId)
        .query((rs, rowNum) -> java.util.Optional.ofNullable(rs.getObject(1)))
        .single()
        .orElse(null);
  }
}
