/// **The suitability hard-gate.**
///
/// SUIT-R20 / INV-QUO-01, control C1, non-waivable.
///
/// > Quote endpoints return 403 unless the request carries a suitability
/// > evaluation id that is ACTIVE, issued within 30 days, bound to the same
/// > customer, and whose outcome permits the requested product class.
///
/// This is the control the programme has already shipped without once: a
/// hardened quote path was delivered with no suitability gate in front of it
/// (`01-POSITION-ASSESSMENT.md` GAP-C). These tests exist so that cannot recur
/// silently in the RM application.
///
/// Three independent layers are asserted:
///
/// 1. **the mint point** — `SuitabilityAssessment.evaluationRef` returns `null`
///    in every case SUIT-R20 forbids;
/// 2. **the router** — navigating to `/quote` renders `GuardBlockedScreen`, not
///    a quote screen;
/// 3. **the repository** — a stale or mismatched token is refused at the point
///    of use.
///
/// The fourth layer is the compiler: `QuoteRepository.requestQuote` takes a
/// `SuitabilityEvaluationRef`, whose constructor is library-private. Code that
/// holds no valid assessment has nothing to pass, so "quote without suitability"
/// is not a test that can fail — it is a program that does not compile. That
/// property is asserted structurally in `structural_guarantees_test.dart`.
library;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:rm_workspace_app/app.dart';
import 'package:rm_workspace_app/data/repositories.dart';
import 'package:rm_workspace_app/domain/ids.dart';
import 'package:rm_workspace_app/domain/suitability.dart';
import 'package:rm_workspace_app/guards/journey_guard.dart';
import 'package:rm_workspace_app/screens/guard_blocked.dart';
import 'package:rm_workspace_app/screens/quote_screens.dart';

import '../support/journey_fixtures.dart';

void main() {
  group('Layer 1 — the capability token cannot be minted', () {
    test('no evaluation exists at all: quote permission is absent', () async {
      final c = await withConsent();
      expect(c.assessment, isNotNull, reason: 'assessment was started');
      expect(c.assessment!.state, AssessmentState.inProgress);
      expect(c.termSuitabilityRef, isNull);
      expect(c.hasQuotePermission, isFalse);
    });

    test('assessment IN_PROGRESS mints no token', () async {
      final c = await withConsent();
      expect(c.assessment!.evaluationRef(ProductClass.term, c.now), isNull);
    });

    test('outcome NOT_SUITABLE mints no token — SUIT-R12, SUIT-R38', () async {
      final c = await withSuitability(answers: unaffordableAnswers);
      final outcome = c.assessment!.outcomeFor(ProductClass.term)!;

      expect(outcome.outcome, SuitabilityOutcome.notSuitable);
      expect(outcome.reasonCodes, contains('PREMIUM_UNAFFORDABLE'));
      expect(c.termSuitabilityRef, isNull,
          reason: 'a NOT_SUITABLE outcome must never permit a quote');
      expect(c.hasQuotePermission, isFalse);
    });

    test('evaluation older than 30 days mints no token — SUIT-R20 condition 2',
        () async {
      var fakeNow = DateTime(2026, 8, 16, 10);
      final c = await withSuitability(clock: () => fakeNow);

      expect(c.hasQuotePermission, isTrue, reason: 'valid on the day');

      fakeNow = fakeNow.add(const Duration(days: 29));
      expect(c.hasQuotePermission, isTrue, reason: 'still inside 30 days');

      fakeNow = fakeNow.add(const Duration(days: 2)); // day 31
      expect(c.termSuitabilityRef, isNull,
          reason: 'past the 30-day window the token must not mint');
      expect(c.hasQuotePermission, isFalse);
    });

    test('a token minted for Term does not authorise ULIP', () async {
      final c = await withSuitability();
      final ulipOutcome = c.assessment!.outcomeFor(ProductClass.ulip)!;

      // These answers are protection-objective, so ULIP is refused.
      expect(ulipOutcome.outcome.permitsQuote, isFalse);
      expect(c.suitabilityRefFor(ProductClass.ulip), isNull);
      expect(c.suitabilityRefFor(ProductClass.term), isNotNull);
    });
  });

  group('Layer 2 — the router refuses the route', () {
    testWidgets('navigating to /quote with no completed evaluation renders '
        'GuardBlockedScreen, not the quote screen', (tester) async {
      final c = await withConsent(); // assessment started, NOT completed

      await tester.pumpWidget(
        RmWorkspaceApp(controller: c, initialRoute: AppRoute.quote),
      );
      await tester.pumpAndSettle();

      // The blocked screen is what rendered.
      expect(find.byType(GuardBlockedScreen), findsOneWidget);

      // The quote screen was never constructed. This is the difference between
      // a structural guard and a disabled button.
      expect(find.byType(QuoteScreen), findsNothing);

      // And the refusal names the rule, in RM-facing language.
      expect(find.textContaining('suitability evaluation is required'),
          findsOneWidget);
      expect(find.textContaining('SUITABILITY_EVALUATION_REQUIRED'),
          findsOneWidget);
    });

    testWidgets('the same applies to /products and /compare', (tester) async {
      for (final route in [AppRoute.products, AppRoute.compare]) {
        final c = await withConsent();
        await tester.pumpWidget(
          RmWorkspaceApp(controller: c, initialRoute: route),
        );
        await tester.pumpAndSettle();
        expect(find.byType(GuardBlockedScreen), findsOneWidget,
            reason: '${route.path} must be gated on suitability too');
      }
    });

    testWidgets('a NOT_SUITABLE outcome still blocks the quote route',
        (tester) async {
      final c = await withSuitability(answers: unaffordableAnswers);

      await tester.pumpWidget(
        RmWorkspaceApp(controller: c, initialRoute: AppRoute.quote),
      );
      await tester.pumpAndSettle();

      expect(find.byType(GuardBlockedScreen), findsOneWidget);
      expect(find.byType(QuoteScreen), findsNothing);
    });

    testWidgets('with a valid evaluation the quote route opens normally',
        (tester) async {
      final c = await withSuitability();
      await c.loadEligibleProducts();
      await c.requestQuote(
        product: c.eligibleProducts.first,
        sumAssured: 5000000,
        termYears: 25,
      );

      await tester.pumpWidget(
        RmWorkspaceApp(controller: c, initialRoute: AppRoute.quote),
      );
      await tester.pumpAndSettle();

      expect(find.byType(QuoteScreen), findsOneWidget);
      expect(find.byType(GuardBlockedScreen), findsNothing);
    });

    testWidgets('the guard is checked directly, not only through navigation',
        (tester) async {
      final c = await withConsent();
      final refusal = JourneyGuard.check(AppRoute.quote, c);

      expect(refusal, isNotNull);
      expect(refusal!.code, 'SUITABILITY_EVALUATION_REQUIRED');
      expect(refusal.rule, contains('SUIT-R20'));
      expect(refusal.rule, contains('non-waivable'));
      expect(refusal.recoverAt, AppRoute.suitability);
    });
  });

  group('Layer 3 — the repository refuses at the point of use', () {
    test('a token that expired after minting is refused when the quote is '
        'actually requested', () async {
      var fakeNow = DateTime(2026, 8, 16, 10);
      final c = await withSuitability(clock: () => fakeNow);

      // Mint while valid — this is what a screen would hold.
      final ref = c.termSuitabilityRef;
      expect(ref, isNotNull);

      // Time passes past the window before the request is made.
      fakeNow = fakeNow.add(const Duration(days: 31));

      await expectLater(
        c.quotes.requestQuote(
          customerId: c.customer!.id,
          suitabilityRef: ref!,
          insurerProductCode: 'SMPL-TERM-01',
          sumAssured: 5000000,
          termYears: 25,
          idempotencyKey: 'k1',
        ),
        throwsA(isA<RepositoryException>().having(
            (e) => e.code, 'code', 'SUITABILITY_EVALUATION_EXPIRED')),
      );
    });

    test('a token bound to another customer is refused — SUIT-R20 condition 3',
        () async {
      final c = await withSuitability();
      final ref = c.termSuitabilityRef!;

      await expectLater(
        c.quotes.requestQuote(
          customerId: const CustomerId('CUST-SOMEONE-ELSE'),
          suitabilityRef: ref,
          insurerProductCode: 'SMPL-TERM-01',
          sumAssured: 5000000,
          termYears: 25,
          idempotencyKey: 'k2',
        ),
        throwsA(isA<RepositoryException>()
            .having((e) => e.code, 'code', 'SUITABILITY_SUBJECT_MISMATCH')),
      );
    });
  });

  group('there is no override path — SUIT-R38, SUIT-R40', () {
    testWidgets('the outcome screen offers no override control for a '
        'NOT_SUITABLE result', (tester) async {
      final c = await withSuitability(answers: unaffordableAnswers);

      await tester.pumpWidget(
        RmWorkspaceApp(controller: c, initialRoute: AppRoute.suitabilityOutcome),
      );
      await tester.pumpAndSettle();

      // The refusal is explained...
      expect(find.textContaining('cannot be overridden'), findsOneWidget);

      // ...and no control exists to act on it.
      for (final label in const [
        'Override',
        'Proceed anyway',
        'Continue anyway',
        'Request exception',
        'Skip suitability',
      ]) {
        expect(find.widgetWithText(InkWell, label), findsNothing,
            reason: 'no "$label" affordance may exist on the outcome screen');
      }
    });
  });
}
