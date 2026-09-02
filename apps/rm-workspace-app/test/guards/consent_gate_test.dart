/// **The consent gate.**
///
/// INV-PRP-01, control C2, non-waivable:
///
/// > A Proposal may leave DRAFT only if a Consent in GRANTED exists for the same
/// > customer, covering the required purpose set, unexpired at submit time.
///
/// Plus SUIT-R21 upstream: a suitability assessment cannot start without the
/// `CNS-SOL` grant.
library;

import 'package:flutter_test/flutter_test.dart';
import 'package:rm_workspace_app/app.dart';
import 'package:rm_workspace_app/data/repositories.dart';
import 'package:rm_workspace_app/domain/consent.dart';
import 'package:rm_workspace_app/domain/ids.dart';
import 'package:rm_workspace_app/guards/journey_guard.dart';
import 'package:rm_workspace_app/screens/guard_blocked.dart';
import 'package:rm_workspace_app/screens/proposal_screens.dart';

import '../support/journey_fixtures.dart';

const c1 = ConsentId('CNS-TEST-1');
const cust1 = CustomerId('CUST-TEST-1');

final evidence = ConsentEvidence(
  statementText: 'placeholder',
  statementVersion: 'CNS-SOL-en-IN-v1.0',
  statementTextHash: 'sha256:test',
  otpTransactionId: 'OTP-TEST',
  otpVerifiedAt: DateTime(2026, 8, 16),
  capturedAt: DateTime(2026, 8, 16),
  capturedByActorId: const ActorId('RM-TEST'),
  agentId: 'SP-TEST',
);

void main() {
  group('the consent grant token cannot be minted without a granted consent',
      () {
    test('a REQUESTED consent mints no token', () async {
      final c = await withLead();
      await c.requestConsents(); // dispatched, not yet verified

      final grant = c.consentGrants[ConsentCode.solicitation]!;
      expect(grant.state, ConsentState.otpPending);
      expect(grant.grantRef(c.now), isNull);
      expect(c.consentRef(ConsentCode.solicitation), isNull);
    });

    test('CNS-R10 — a consent cannot be granted without a verified OTP', () {
      final grant = ConsentGrant.request(
        id: c1,
        code: ConsentCode.solicitation,
        customerId: cust1,
      ).challenge();

      expect(
        () => grant.verifyOtp(
          otpTransactionId: '', // no verified transaction
          now: DateTime(2026, 8, 16),
          evidence: evidence,
        ),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('INV-CNS-02 — GRANTED is reachable only from OTP_PENDING', () {
      final requested = ConsentGrant.request(
        id: c1,
        code: ConsentCode.solicitation,
        customerId: cust1,
      );

      expect(
        () => requested.verifyOtp(
          otpTransactionId: 'OTP-1',
          now: DateTime(2026, 8, 16),
          evidence: evidence,
        ),
        throwsA(isA<StateError>()),
        reason: 'REQUESTED -> GRANTED is not a legal transition',
      );
    });

    test('a withdrawn consent mints no token, and the evidence survives',
        () async {
      final c = await withConsent();
      final granted = c.consentGrants[ConsentCode.solicitation]!;
      expect(granted.grantRef(c.now), isNotNull);

      final withdrawn = granted.withdraw();
      expect(withdrawn.state, ConsentState.withdrawn);
      expect(withdrawn.grantRef(c.now), isNull);

      // CNS-R32: withdrawal never deletes prior evidence.
      expect(withdrawn.evidence, isNotNull);
      expect(withdrawn.evidence!.otpTransactionId, isNotEmpty);
      expect(withdrawn.evidence!.statementVersion, isNotEmpty);
    });

    test('CNS-R22 — a sharing consent expires after 30 days', () async {
      var fakeNow = DateTime(2026, 8, 16, 10);
      final c = await withSharingConsent(clock: () => fakeNow);

      expect(c.hasSharingConsent, isTrue);

      fakeNow = fakeNow.add(const Duration(days: 31));
      expect(c.consentRef(ConsentCode.sharing), isNull);
      expect(c.hasSharingConsent, isFalse);
    });
  });

  group('the router refuses the proposal route without sharing consent', () {
    testWidgets('/submit renders GuardBlockedScreen when sharing consent is '
        'absent', (tester) async {
      final c = await withSelectedOffer(); // no CNS-SHR yet

      await tester.pumpWidget(
        RmWorkspaceApp(controller: c, initialRoute: AppRoute.submit),
      );
      await tester.pumpAndSettle();

      expect(find.byType(GuardBlockedScreen), findsOneWidget);
      expect(find.byType(ProposalSubmitScreen), findsNothing);
      expect(find.textContaining('consent to their details being shared'),
          findsOneWidget);
    });

    testWidgets('with sharing consent, /submit opens', (tester) async {
      final c = await withSharingConsent();
      await c.createProposal();

      await tester.pumpWidget(
        RmWorkspaceApp(controller: c, initialRoute: AppRoute.submit),
      );
      await tester.pumpAndSettle();

      expect(find.byType(ProposalSubmitScreen), findsOneWidget);
      expect(find.byType(GuardBlockedScreen), findsNothing);
    });

    test('the guard names the non-waivable rule', () async {
      final c = await withSelectedOffer();
      final refusal = JourneyGuard.check(AppRoute.submit, c);

      expect(refusal, isNotNull);
      expect(refusal!.code, 'CONSENT_REQUIRED');
      expect(refusal.rule, contains('INV-PRP-01'));
      expect(refusal.rule, contains('non-waivable'));
    });
  });

  group('the repository refuses submission at the point of use', () {
    test('an expired sharing consent is refused on submit', () async {
      var fakeNow = DateTime(2026, 8, 16, 10);
      final c = await withSharingConsent(clock: () => fakeNow);
      await c.createProposal();

      final ref = c.consentRef(ConsentCode.sharing)!;
      fakeNow = fakeNow.add(const Duration(days: 31));

      await expectLater(
        c.proposals.submit(
          id: c.proposal!.id,
          sharingConsent: ref,
          agentId: 'SP-DEMO-778812',
          now: fakeNow,
        ),
        throwsA(isA<RepositoryException>()
            .having((e) => e.code, 'code', 'CONSENT_EXPIRED')),
      );
    });

    test('a consent of the wrong scope is refused — CNS-R22', () async {
      final c = await withSharingConsent();
      await c.createProposal();

      // A data-processing grant is a valid grant; it is not sharing consent.
      final wrongScope = c.consentRef(ConsentCode.dataProcessing)!;

      await expectLater(
        c.proposals.submit(
          id: c.proposal!.id,
          sharingConsent: wrongScope,
          agentId: 'SP-DEMO-778812',
          now: c.now,
        ),
        throwsA(isA<RepositoryException>()
            .having((e) => e.code, 'code', 'CONSENT_SCOPE_MISMATCH')),
      );
    });

    test('AC-PROP-030-1 — no agentId, no insurer call', () async {
      final c = await withSharingConsent();
      await c.createProposal();

      await expectLater(
        c.proposals.submit(
          id: c.proposal!.id,
          sharingConsent: c.consentRef(ConsentCode.sharing)!,
          agentId: '',
          now: c.now,
        ),
        throwsA(isA<RepositoryException>()
            .having((e) => e.code, 'code', 'AGENT_ATTRIBUTION_MISSING')),
      );
    });
  });

  group('SUIT-R21 — suitability requires CNS-SOL upstream', () {
    testWidgets('/suitability is refused without the solicitation consent',
        (tester) async {
      final c = await withLead(); // no consents captured

      await tester.pumpWidget(
        RmWorkspaceApp(controller: c, initialRoute: AppRoute.suitability),
      );
      await tester.pumpAndSettle();

      expect(find.byType(GuardBlockedScreen), findsOneWidget);
    });

    test('startSuitability fails without the solicitation grant', () async {
      final c = await withLead();
      await c.startSuitability();
      expect(c.assessment, isNull);
      expect(c.lastError, contains('CONSENT_REQUIRED'));
    });
  });
}
