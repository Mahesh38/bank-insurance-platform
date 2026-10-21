package com.bank.platform.proposal.adapter.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.proposal.adapter.crypto.AesGcmEnvelopeCipher;
import com.bank.platform.proposal.adapter.crypto.LocalDataKeyProvider;
import com.bank.platform.proposal.adapter.envelope.ProposalEnvelopeCodec;
import com.bank.platform.proposal.domain.CreateOutcome;
import com.bank.platform.proposal.domain.IdempotencyClaim;
import com.bank.platform.proposal.domain.NewProposal;
import com.bank.platform.proposal.domain.OutboxEvent;
import com.bank.platform.proposal.domain.ProposalState;
import com.bank.platform.proposal.domain.StoreGuard;
import com.bank.platform.proposal.domain.Ulid;
import com.bank.platform.proposal.domain.WorkingProposal;
import com.bank.platform.proposal.domain.WorkingRetention;
import com.bank.platform.proposal.support.LocalDynamoDb;
import com.bank.platform.proposal.support.ProposalFixtures;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Against in-process DynamoDB Local: real condition expressions and transactions. */
class DynamoWorkingProposalStoreTest {

  private static final Instant NOW = Instant.parse("2026-10-21T10:00:00.250Z");
  private static final Duration WORKING = Duration.ofDays(90);

  private final DynamoDbClient dynamo = LocalDynamoDb.client();
  private final DynamoWorkingProposalStore store =
      new DynamoWorkingProposalStore(
          dynamo,
          new ProposalTableSettings(
              LocalDynamoDb.TABLE,
              new WorkingRetention(WORKING, Duration.ofDays(30)),
              Duration.ofHours(24),
              Duration.ofDays(7)),
          new AesGcmEnvelopeCipher(
              new LocalDataKeyProvider("p7MSmdyOLeyBue5/ZJpPP3TE/ZQrLr4WSPjrhS0d0F8=", "test")),
          new ProposalEnvelopeCodec(),
          ServiceErrors.of("proposal", PlatformLayer.L5));

  private WorkingProposal created(NewProposal request) {
    WorkingProposal p = WorkingProposal.draft(Ulid.mint(), request, NOW);
    store.create(p, new IdempotencyClaim(Ulid.mint(), "hash"), event(p));
    return p;
  }

  private static OutboxEvent event(WorkingProposal p) {
    return new OutboxEvent(Ulid.mint(), "ProposalCreated", p.proposalId(), NOW, Map.of());
  }

  private Map<String, AttributeValue> item(String pk, String sk) {
    return dynamo
        .getItem(
            r ->
                r.tableName(LocalDynamoDb.TABLE)
                    .key(DynamoWorkingProposalStore.key(pk, sk))
                    .consistentRead(true))
        .item();
  }

  private String code(Throwable e) {
    return ((ServiceException) e).getErrorResponse().getCode();
  }

  private WorkingProposal answered() {
    WorkingProposal p = created(ProposalFixtures.newProposal());
    p.saveAnswers(ProposalFixtures.answers(), ProposalFixtures.nominee(), NOW);
    store.update(p, StoreGuard.NONE, null);
    return p;
  }

  private WorkingProposal awaitingPayment(String applicationNumber) {
    WorkingProposal p = answered();
    p.submit(NOW);
    store.update(p, StoreGuard.NONE, event(p));
    p.acknowledge(applicationNumber, NOW);
    store.update(p, StoreGuard.NONE, null);
    p.recordUnderwriting(ProposalState.UW_APPROVED, "APPROVED", null, null, NOW);
    store.update(p, StoreGuard.NONE, null);
    p.awaitPayment(NOW);
    store.update(p, StoreGuard.NONE, null);
    return p;
  }

  @Test
  void createWritesMetaIdemMarkerAndOutbox() {
    NewProposal request = ProposalFixtures.newProposal();
    WorkingProposal p = WorkingProposal.draft(Ulid.mint(), request, NOW);
    OutboxEvent event = event(p);

    store.create(p, new IdempotencyClaim("k-" + p.proposalId(), "h"), event);

    Map<String, AttributeValue> meta = item("PROPOSAL#" + p.proposalId(), "META");
    assertThat(meta.get("state").s()).isEqualTo("DRAFT");
    assertThat(meta.get("version").n()).isEqualTo("1");
    assertThat(meta.get("schemaId").s()).isEqualTo("INS01-TERM-FORM");
    assertThat(meta.get("ttl").n()).isEqualTo(Long.toString(NOW.plus(WORKING).getEpochSecond()));
    assertThat(item("IDEM#k-" + p.proposalId(), "IDEM").get("responseRef").s())
        .isEqualTo(p.proposalId());
    assertThat(item("JOURNEY#" + request.journeyId(), "OPEN_PROPOSAL").get("proposalId").s())
        .isEqualTo(p.proposalId());
    assertThat(item("PROPOSAL#" + p.proposalId(), "OUTBOX#" + event.eventId())).isNotEmpty();
  }

  @Test
  void idempotentReplayAndConflictingReplay() {
    NewProposal request = ProposalFixtures.newProposal();
    WorkingProposal first = WorkingProposal.draft(Ulid.mint(), request, NOW);
    String key = "k-" + first.proposalId();
    store.create(first, new IdempotencyClaim(key, "same"), event(first));

    WorkingProposal retry = WorkingProposal.draft(Ulid.mint(), request, NOW);
    assertThat(store.create(retry, new IdempotencyClaim(key, "same"), event(retry)))
        .isEqualTo(new CreateOutcome(first.proposalId(), true));

    WorkingProposal other = WorkingProposal.draft(Ulid.mint(), request, NOW);
    assertThatThrownBy(() -> store.create(other, new IdempotencyClaim(key, "other"), event(other)))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.IDEMPOTENCY_CONFLICT));
  }

  @Test
  void oneOpenProposalPerJourney() {
    String journeyId = Ulid.mint();
    created(ProposalFixtures.newProposal(journeyId));

    assertThatThrownBy(() -> created(ProposalFixtures.newProposal(journeyId)))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.CONFLICT));
  }

  @Test
  void staleVersionIsConflict() {
    WorkingProposal p = created(ProposalFixtures.newProposal());
    WorkingProposal stale = store.find(p.proposalId()).orElseThrow();
    p.saveAnswers(Map.of("a", "1"), null, NOW);
    store.update(p, StoreGuard.NONE, null);

    stale.saveAnswers(Map.of("a", "2"), null, NOW);

    assertThatThrownBy(() -> store.update(stale, StoreGuard.NONE, null))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.CONFLICT));
    assertThat(store.find(p.proposalId()).orElseThrow().envelope().formValues())
        .containsEntry("a", "1");
  }

  @Test
  void nomineeAndFormValuesAreOnlyCiphertext() {
    WorkingProposal p = answered();

    Map<String, AttributeValue> meta = item("PROPOSAL#" + p.proposalId(), "META");
    String raw = new String(meta.get("payloadEnc").b().asByteArray(), StandardCharsets.ISO_8859_1);

    assertThat(raw)
        .doesNotContain(ProposalFixtures.NOMINEE_NAME)
        .doesNotContain("Asha")
        .doesNotContain(ProposalFixtures.PAN);
    assertThat(meta.toString())
        .doesNotContain(ProposalFixtures.NOMINEE_NAME)
        .doesNotContain(ProposalFixtures.PAN);
    WorkingProposal read = store.find(p.proposalId()).orElseThrow();
    assertThat(read.envelope().nominee().name()).isEqualTo(ProposalFixtures.NOMINEE_NAME);
    assertThat(read.envelope().formValues())
        .containsEntry("proposer.panNumber", ProposalFixtures.PAN);
  }

  @Test
  void applicationNumberIsUniqueAcrossProposals() {
    String appNo = "APP-" + Ulid.mint();
    WorkingProposal first = awaitingPayment(appNo);
    assertThat(item("APPNO#" + appNo, "APPNO").get("proposalId").s()).isEqualTo(first.proposalId());
    assertThat(item("PROPOSAL#" + first.proposalId(), "META").get("applicationNumber").s())
        .isEqualTo(appNo);

    WorkingProposal second = answered();
    second.submit(NOW);
    store.update(second, StoreGuard.NONE, null);
    second.acknowledge(appNo, NOW);

    assertThatThrownBy(() -> store.update(second, StoreGuard.NONE, null))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.CONFLICT));
  }

  @Test
  void ttlIsRemovedWhilePaymentInFlightAndRestoredAfter() {
    WorkingProposal p = awaitingPayment("APP-" + Ulid.mint());
    String pk = "PROPOSAL#" + p.proposalId();

    p.markPaymentInFlight(NOW);
    store.update(p, StoreGuard.AWAITING_PAYMENT, null);
    assertThat(item(pk, "META")).doesNotContainKey("ttl");

    p.paymentEnded(NOW);
    store.update(p, StoreGuard.AWAITING_PAYMENT, null);
    assertThat(item(pk, "META").get("ttl").n())
        .isEqualTo(Long.toString(NOW.plus(WORKING).getEpochSecond()));
  }

  @Test
  void guardFailureIsIllegalTransition() {
    WorkingProposal p = answered();
    // in memory the state is DRAFT, so the AWAITING_PAYMENT guard cannot hold in the table
    p.saveAnswers(Map.of("x", "y"), null, NOW);

    assertThatThrownBy(() -> store.update(p, StoreGuard.AWAITING_PAYMENT, null))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.ILLEGAL_TRANSITION));
  }

  @Test
  void materializedItemIsFrozenAndWithdrawalReleasesMarker() {
    WorkingProposal p = awaitingPayment("APP-" + Ulid.mint());
    WorkingProposal stale = store.find(p.proposalId()).orElseThrow();
    p.materialize(NOW);

    assertThat(store.markMaterialized(p)).isTrue();
    assertThat(store.markMaterialized(p)).isFalse();
    assertThat(item("PROPOSAL#" + p.proposalId(), "META").get("state").s()).isEqualTo("PAID");
    stale.paymentEnded(NOW);
    assertThatThrownBy(() -> store.update(stale, StoreGuard.NONE, null))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.ILLEGAL_TRANSITION));

    String journeyId = Ulid.mint();
    WorkingProposal withdrawn = created(ProposalFixtures.newProposal(journeyId));
    withdrawn.withdraw(NOW);
    store.update(withdrawn, StoreGuard.NONE, null);
    assertThat(item("JOURNEY#" + journeyId, "OPEN_PROPOSAL")).isNullOrEmpty();
    store.releaseOpenMarker(journeyId, withdrawn.proposalId());
  }

  @Test
  void updateOfMissingProposalIsNotFound() {
    WorkingProposal ghost = WorkingProposal.draft(Ulid.mint(), ProposalFixtures.newProposal(), NOW);
    ghost.markPersisted(1, "local:test");

    assertThatThrownBy(() -> store.update(ghost, StoreGuard.NONE, null))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND));
  }
}
