package com.bank.platform.proposal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.platform.proposal.support.ProposalFixtures;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkingProposalTest {

  private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

  private static String code(Throwable e) {
    return ((ProposalRuleException) e).code();
  }

  private WorkingProposal approved() {
    WorkingProposal p = WorkingProposal.draft("P1", ProposalFixtures.newProposal(), NOW);
    p.saveAnswers(ProposalFixtures.answers(), ProposalFixtures.nominee(), NOW);
    p.submit(NOW);
    p.acknowledge("APP123456", NOW);
    p.recordUnderwriting(
        ProposalState.UW_APPROVED, "APPROVED", List.of(ProposalFixtures.requirement()), null, NOW);
    return p;
  }

  @Test
  void answersMergeAndOnlyDraftsAcceptThem() {
    WorkingProposal p = WorkingProposal.draft("P1", ProposalFixtures.newProposal(), NOW);
    p.saveAnswers(ProposalFixtures.answers(Map.of("a", "1")), null, NOW);
    p.saveAnswers(ProposalFixtures.answers(Map.of("b", "2")), ProposalFixtures.nominee(), NOW);

    assertThat(ProposalFixtures.values(p.envelope()))
        .containsEntry("a", "1")
        .containsEntry("b", "2");
    assertThat(p.envelope().nominee().name()).isEqualTo(ProposalFixtures.NOMINEE_NAME);

    p.submit(NOW);
    assertThatThrownBy(() -> p.saveAnswers(ProposalFixtures.answers(Map.of()), null, NOW))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.ILLEGAL_TRANSITION));
  }

  @Test
  void acknowledgementClaimsTheApplicationNumberOnce() {
    WorkingProposal p = WorkingProposal.draft("P1", ProposalFixtures.newProposal(), NOW);
    p.submit(NOW);
    assertThatThrownBy(() -> p.acknowledge(" ", NOW))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.MISSING_REQUIRED_FIELD));

    p.acknowledge("APP1", NOW);

    assertThat(p.state()).isEqualTo(ProposalState.UNDER_WRITING);
    assertThat(p.unclaimedApplicationNumber()).isEqualTo("APP1");
    p.markPersisted(2, "k");
    assertThat(p.unclaimedApplicationNumber()).isNull();
    assertThat(p.applicationNumber()).isEqualTo("APP1");
  }

  @Test
  void underwritingOnlyMovesToInsurerOutcomes() {
    WorkingProposal p = WorkingProposal.draft("P1", ProposalFixtures.newProposal(), NOW);
    assertThatThrownBy(() -> p.recordUnderwriting(ProposalState.UW_APPROVED, "X", null, null, NOW))
        .isInstanceOf(ProposalRuleException.class);
    p.submit(NOW);
    p.acknowledge("APP1", NOW);
    assertThatThrownBy(() -> p.recordUnderwriting(ProposalState.PAID, "X", null, null, NOW))
        .isInstanceOf(ProposalRuleException.class);

    p.recordUnderwriting(
        ProposalState.REQUIREMENTS_PENDING,
        "REQUIREMENTS_PENDING",
        List.of(ProposalFixtures.requirement()),
        List.of(ProposalFixtures.document()),
        NOW);

    assertThat(p.envelope().requirements()).hasSize(1);
    assertThat(p.envelope().documents()).hasSize(1);
  }

  @Test
  void withdrawalIsRefusedOnceAwaitingPayment() {
    WorkingProposal draft = WorkingProposal.draft("P1", ProposalFixtures.newProposal(), NOW);
    draft.withdraw(NOW);
    assertThat(draft.state()).isEqualTo(ProposalState.WITHDRAWN);
    assertThat(draft.state().endsWithoutSale()).isTrue();

    WorkingProposal p = approved();
    p.awaitPayment(NOW);
    assertThatThrownBy(() -> p.withdraw(NOW))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.WITHDRAWAL_NOT_PERMITTED));
  }

  @Test
  void paymentAndMaterializeFreezeTheProposal() {
    WorkingProposal p = approved();
    assertThatThrownBy(() -> p.markPaymentInFlight(NOW)).isInstanceOf(ProposalRuleException.class);
    p.awaitPayment(NOW);

    p.markPaymentInFlight(NOW);
    assertThat(p.paymentInFlight()).isTrue();
    p.paymentEnded(NOW);
    assertThat(p.paymentInFlight()).isFalse();
    p.materialize(NOW);

    assertThat(p.state()).isEqualTo(ProposalState.PAID);
    assertThat(p.isMaterialized()).isTrue();
    assertThatThrownBy(() -> p.paymentEnded(NOW))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.ILLEGAL_TRANSITION));
  }

  @Test
  void e1PaymentOnlyAfterUnderwritingApproval() {
    for (ProposalState uw :
        List.of(
            ProposalState.UNDER_WRITING,
            ProposalState.REQUIREMENTS_PENDING,
            ProposalState.UW_COUNTER_OFFER)) {
      WorkingProposal p = WorkingProposal.draft("P1", ProposalFixtures.newProposal(), NOW);
      p.submit(NOW);
      p.acknowledge("APP-" + uw, NOW);
      if (uw != ProposalState.UNDER_WRITING) {
        p.recordUnderwriting(uw, uw.name(), null, null, NOW);
      }
      assertThatThrownBy(() -> p.awaitPayment(NOW))
          .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.ILLEGAL_TRANSITION));
      assertThatThrownBy(() -> p.holdForPayment("PAY1", NOW))
          .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.PROPOSAL_NOT_PAYABLE));
    }
  }

  @Test
  void counterOfferIsKeptAndAcceptedIntoApproval() {
    WorkingProposal p = WorkingProposal.draft("P1", ProposalFixtures.newProposal(), NOW);
    p.saveAnswers(ProposalFixtures.answers(), ProposalFixtures.nominee(), NOW);
    p.submit(NOW);
    p.acknowledge("APP-CO", NOW);
    CounterOffer offer =
        new CounterOffer("CO1", new java.math.BigDecimal("15000.00"), null, "LOADING", null);
    p.recordUnderwriting(ProposalState.UW_COUNTER_OFFER, "COUNTER_OFFER", null, null, offer, NOW);

    p.decideCounterOffer(true, NOW);

    assertThat(p.state()).isEqualTo(ProposalState.UW_APPROVED);
    assertThat(p.envelope().counterOffer()).isEqualTo(offer);
    assertThatThrownBy(() -> p.decideCounterOffer(false, NOW))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.ILLEGAL_TRANSITION));
  }

  @Test
  void holdAndReleaseArePerPayment() {
    WorkingProposal p = approved();
    p.awaitPayment(NOW);

    assertThat(p.holdForPayment("PAY1", NOW)).isTrue();
    assertThat(p.holdForPayment("PAY1", NOW)).isFalse();
    assertThatThrownBy(() -> p.holdForPayment("PAY2", NOW))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.PAYMENT_ALREADY_IN_PROGRESS));
    assertThat(p.releasePaymentHold("PAY2", NOW)).isFalse();
    assertThat(p.releasePaymentHold("PAY1", NOW)).isTrue();
    assertThat(p.releasePaymentHold("PAY1", NOW)).isFalse();
    assertThat(p.state()).isEqualTo(ProposalState.AWAITING_PAYMENT);
  }

  @Test
  void answersNeedTheQuestionAsShownAndV1AnswersCannotMaterialise() {
    WorkingProposal p = WorkingProposal.draft("P1", ProposalFixtures.newProposal(), NOW);
    assertThatThrownBy(
            () ->
                p.saveAnswers(
                    List.of(new ProposalAnswer("q1", null, null, "3", "v", null, null)), null, NOW))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.VALIDATION_ERROR));

    WorkingProposal.Snapshot s = approved().snapshot();
    WorkingProposal legacy =
        WorkingProposal.restore(
            new WorkingProposal.Snapshot(
                s.proposalId(),
                s.leadId(),
                s.journeyId(),
                s.customerId(),
                s.quoteId(),
                s.offerId(),
                s.consentId(),
                s.lob(),
                s.applicationNumber(),
                s.schemaId(),
                s.schemaVersion(),
                ProposalState.AWAITING_PAYMENT,
                s.underwritingState(),
                s.issuanceMode(),
                s.configVersionRef(),
                false,
                null,
                null,
                1,
                s.createdAt(),
                s.updatedAt(),
                new ProposalEnvelope(
                    List.of(new ProposalAnswer("q1", null, null, "3", "v", null, null)),
                    null,
                    List.of(),
                    List.of(),
                    null),
                "k"));
    assertThatThrownBy(() -> legacy.materialize(NOW))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.INVALID_STATE));
  }
}
