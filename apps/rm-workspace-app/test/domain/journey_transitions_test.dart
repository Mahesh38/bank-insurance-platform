/// Journey saga transitions — INV-JRN-01 and INV-JRN-04.
///
/// The legal transition set is transcribed from `01-domain-model-and-invariants.md`
/// §5. A transition not drawn there is illegal and must be rejected by the
/// aggregate, not by the caller.
library;

import 'package:flutter_test/flutter_test.dart';
import 'package:rm_workspace_app/domain/consent.dart';
import 'package:rm_workspace_app/domain/ids.dart';
import 'package:rm_workspace_app/domain/journey.dart';
import 'package:rm_workspace_app/domain/sales.dart';

import '../support/journey_fixtures.dart';

void main() {
  group('INV-JRN-01 — only drawn transitions are legal', () {
    test('the happy path is legal end to end', () {
      for (var i = 0; i < kHappyPath.length - 1; i++) {
        expect(
          isLegalTransition(kHappyPath[i], kHappyPath[i + 1]),
          isTrue,
          reason: '${kHappyPath[i].name} -> ${kHappyPath[i + 1].name} '
              'must be legal',
        );
      }
    });

    test('skipping suitability is not a legal transition', () {
      // The stage machine cannot express "need analysis straight to quoting".
      expect(
        isLegalTransition(JourneyStage.needAnalysis, JourneyStage.quoting),
        isFalse,
      );
    });

    test('skipping consent is not a legal transition', () {
      expect(
        isLegalTransition(
            JourneyStage.suitabilityComplete, JourneyStage.quoting),
        isFalse,
      );
    });

    test('skipping payment is not a legal transition', () {
      expect(
        isLegalTransition(JourneyStage.underwriting, JourneyStage.issued),
        isFalse,
      );
      expect(
        isLegalTransition(JourneyStage.paymentPending, JourneyStage.issued),
        isFalse,
      );
    });

    test('terminal stages accept nothing further', () {
      for (final terminal in [
        JourneyStage.sold,
        JourneyStage.abandoned,
        JourneyStage.declined,
        JourneyStage.compensated,
      ]) {
        expect(terminal.isTerminal, isTrue);
        for (final to in JourneyStage.values) {
          expect(isLegalTransition(terminal, to), isFalse,
              reason: '${terminal.name} must accept no transition');
        }
      }
    });
  });

  group('INV-JRN-04 — a compensating journey is never silently closed', () {
    test('COMPENSATING exits only to SOLD, COMPENSATED or MANUAL_INTERVENTION',
        () {
      final exits = kLegalTransitions[JourneyStage.compensating]!;
      expect(exits, {
        JourneyStage.sold,
        JourneyStage.compensated,
        JourneyStage.manualIntervention,
      });
      expect(exits.contains(JourneyStage.abandoned), isFalse,
          reason: 'a compensating journey must never be abandoned');
    });

    test('the post-payment failure path exists', () {
      // F-05: payment captured, issuance fails.
      expect(
        isLegalTransition(
            JourneyStage.paymentSettled, JourneyStage.compensating),
        isTrue,
      );
      expect(
        isLegalTransition(
            JourneyStage.issuancePending, JourneyStage.compensating),
        isTrue,
      );
      // F-06: issued, confirmation never received.
      expect(
        isLegalTransition(JourneyStage.issued, JourneyStage.compensating),
        isTrue,
      );
    });
  });

  group('every stage has a label and the map is total', () {
    test('kLegalTransitions covers every stage', () {
      for (final s in JourneyStage.values) {
        expect(kLegalTransitions.containsKey(s), isTrue,
            reason: '${s.name} has no entry in the transition map');
        expect(stageLabel(s), isNotEmpty);
      }
    });
  });

  group('AC-POL-010-2 / INV-JRN-05 — "sold" requires all four conditions', () {
    Policy policy({
      String? number = 'PN-1',
      bool confirmed = true,
      bool reconciled = true,
      bool audited = true,
      PolicyState state = PolicyState.active,
    }) =>
        Policy(
          id: const PolicyId('POL-1'),
          proposalId: const ProposalId('PROP-1'),
          state: state,
          policyNumber: number,
          issuanceConfirmed: confirmed,
          paymentReconciled: reconciled,
          auditPersisted: audited,
        );

    test('all four present: sold', () {
      expect(policy().isSold, isTrue);
    });

    test('three of four is not sold — in every combination', () {
      expect(policy(number: null).isSold, isFalse, reason: 'no policy number');
      expect(policy(confirmed: false).isSold, isFalse,
          reason: 'issuance not confirmed');
      expect(policy(reconciled: false).isSold, isFalse,
          reason: 'payment not reconciled');
      expect(policy(audited: false).isSold, isFalse,
          reason: 'not persisted in the audit store');
      expect(policy(state: PolicyState.confirmed).isSold, isFalse,
          reason: 'policy not ACTIVE');
    });
  });

  group('INV-POL-01 — issuance requires a reconciled payment', () {
    test('only RECONCILED permits issuance', () {
      for (final state in PaymentState.values) {
        final p = Payment(
          id: const PaymentId('PAY-1'),
          proposalId: const ProposalId('PROP-1'),
          state: state,
          amount: 1000,
        );
        expect(p.permitsIssuance, state == PaymentState.reconciled,
            reason: '${state.name} must ${state == PaymentState.reconciled ? '' : 'not '}'
                'permit issuance');
      }
    });
  });

  group('INV-PRP-04 — withdrawal is barred once a payment window is open', () {
    test('withdrawal is permitted only before AWAITING_PAYMENT', () async {
      // A Proposal requires a ConsentGrantRef, and one cannot be fabricated —
      // its constructor is library-private. So the fixture mints a real one by
      // actually granting consent, which is the point of the guard.
      final c = await withSharingConsent();
      final consentRef = c.consentRef(ConsentCode.sharing)!;

      Proposal at(ProposalState s) => Proposal(
            id: const ProposalId('PROP-1'),
            quoteId: const QuoteId('Q-1'),
            offerId: const OfferId('O-1'),
            customerId: const CustomerId('C-1'),
            sharingConsent: consentRef,
            state: s,
          );

      expect(at(ProposalState.draft).mayWithdraw, isTrue);
      expect(at(ProposalState.submitted).mayWithdraw, isTrue);
      expect(at(ProposalState.underWriting).mayWithdraw, isTrue);
      expect(at(ProposalState.awaitingPayment).mayWithdraw, isFalse);
      expect(at(ProposalState.paid).mayWithdraw, isFalse);
      expect(at(ProposalState.converted).mayWithdraw, isFalse);
    });
  });
}
