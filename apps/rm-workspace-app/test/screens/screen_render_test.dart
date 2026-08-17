/// Widget tests — every R0 screen renders, in its meaningful states.
///
/// These are the least interesting tests in the suite and they are here for a
/// reason: `S05-experience-evidence.md` §4.5 catalogues four states per screen,
/// and a state catalogue nothing exercises is a document, not a specification.
library;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:rm_workspace_app/app.dart';
import 'package:rm_workspace_app/design/components.dart';
import 'package:rm_workspace_app/guards/journey_guard.dart';
import 'package:rm_workspace_app/state/journey_controller.dart';

import '../support/journey_fixtures.dart';

Future<void> _pump(WidgetTester tester, JourneyController c, AppRoute route) async {
  await tester.pumpWidget(RmWorkspaceApp(controller: c, initialRoute: route));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('SCR-01 login renders and signs in', (tester) async {
    final c = JourneyController.withFake();
    await _pump(tester, c, AppRoute.login);

    expect(find.text('SCR-01'), findsOneWidget);
    expect(find.text('Sign in with Bank SSO'), findsOneWidget);
    expect(c.signedIn, isFalse);

    await tester.tap(find.text('Sign in with Bank SSO'));
    await tester.pumpAndSettle();
    expect(c.signedIn, isTrue);
  });

  testWidgets('SCR-02 pipeline — empty state carries an explanation and an '
      'action', (tester) async {
    final c = await signedIn();
    await _pump(tester, c, AppRoute.pipeline);

    expect(find.text('SCR-02'), findsOneWidget);
    expect(find.byType(EmptyState), findsOneWidget);
    expect(find.text('No journeys yet.'), findsOneWidget);
    expect(find.text('Search for a customer'), findsOneWidget);
  });

  testWidgets('SCR-02 pipeline — populated state shows the stage',
      (tester) async {
    final c = await withLead();
    await _pump(tester, c, AppRoute.pipeline);

    expect(find.byType(EmptyState), findsNothing);
    expect(find.textContaining('Meera'), findsOneWidget);
    expect(find.byType(StatusChip), findsWidgets);
  });

  testWidgets('SCR-03 customer search — empty state on no match',
      (tester) async {
    final c = await signedIn();
    await _pump(tester, c, AppRoute.customerSearch);

    expect(find.text('SCR-03'), findsOneWidget);

    await tester.enterText(find.byType(TextField), 'zzzz-no-such-customer');
    await tester.tap(find.text('Search'));
    await tester.pumpAndSettle();

    expect(find.text('No customer found.'), findsOneWidget);
  });

  testWidgets('SCR-03 customer search — results render', (tester) async {
    final c = await signedIn();
    await _pump(tester, c, AppRoute.customerSearch);

    await tester.enterText(find.byType(TextField), 'AU100');
    await tester.tap(find.text('Search'));
    await tester.pumpAndSettle();

    expect(find.textContaining('Meera'), findsOneWidget);
    expect(find.text('Existing customer'), findsWidgets);
  });

  testWidgets('SCR-04 customer detail shows a masked mobile only',
      (tester) async {
    final c = await signedIn();
    final results = await c.searchCustomers('AU100');
    await c.selectCustomer(results.first);
    await _pump(tester, c, AppRoute.customerDetail);

    expect(find.text('SCR-04'), findsOneWidget);
    expect(find.textContaining('••••'), findsWidgets);
  });

  testWidgets('SCR-06 consent — no OTP entry field exists on the RM device',
      (tester) async {
    final c = await withLead();
    await _pump(tester, c, AppRoute.consent);

    expect(find.text('SCR-06'), findsOneWidget);

    // CNS-R13: the RM cannot enter the customer's code. Before dispatch there
    // is no input at all.
    expect(find.byType(TextField), findsNothing);

    await tester.tap(find.text('Send consent request to the customer'));
    await tester.pumpAndSettle();

    // And after dispatch there is still no input — only a waiting panel.
    expect(find.byType(TextField), findsNothing);
    expect(find.byType(WaitingPanel), findsOneWidget);
    expect(find.textContaining('cannot enter that code here'), findsOneWidget);
  });

  testWidgets('SCR-08 questionnaire — the action is unavailable until all '
      'twelve inputs are answered', (tester) async {
    final c = await withConsent();
    await _pump(tester, c, AppRoute.suitability);

    expect(find.text('SCR-08'), findsOneWidget);
    expect(find.text('0 of 12 answered'), findsOneWidget);

    // SUIT-R01 in the UI: nothing has been answered, so nothing can be
    // evaluated. There is no skip affordance to find.
    expect(find.text('Skip'), findsNothing);
    expect(find.text('Skip for now'), findsNothing);
  });

  testWidgets('SCR-09 outcome renders the evaluation and every reason code',
      (tester) async {
    final c = await withSuitability();
    await _pump(tester, c, AppRoute.suitabilityOutcome);

    expect(find.text('SCR-09'), findsOneWidget);
    expect(find.textContaining('SUIT-ALGO-LIFE-v1.0'), findsOneWidget);
    expect(find.text('Suitable'), findsWidgets);
    expect(find.textContaining('PROTECTION_GAP_PRESENT'), findsOneWidget);
  });

  testWidgets('SCR-10 eligible products lists the R0 catalogue',
      (tester) async {
    final c = await withSuitability();
    await c.loadEligibleProducts();
    await _pump(tester, c, AppRoute.products);

    expect(find.text('SCR-10'), findsOneWidget);
    expect(find.textContaining('sample'), findsWidgets);
    expect(find.textContaining('pending commercial confirmation'),
        findsOneWidget);
  });

  testWidgets('SCR-12 compare discloses the ranking rule — QR-06',
      (tester) async {
    final c = await withSelectedOffer();
    await _pump(tester, c, AppRoute.compare);

    expect(find.text('SCR-12'), findsOneWidget);
    expect(
      find.textContaining('Ordered by annual premium, lowest first'),
      findsOneWidget,
    );
    expect(
      find.textContaining('Nothing else influences this order'),
      findsOneWidget,
    );
  });

  testWidgets('SCR-14 submit shows the attribution and evidence trail',
      (tester) async {
    final c = await withSharingConsent();
    await c.createProposal();
    await _pump(tester, c, AppRoute.submit);

    expect(find.text('SCR-14'), findsOneWidget);
    expect(find.text('SP licence'), findsOneWidget);
    expect(find.textContaining('Injected server-side'), findsOneWidget);
    expect(find.text('Suitability evaluation'), findsOneWidget);
  });

  testWidgets('SCR-16 payment shows a link and a waiting state', (tester) async {
    final c = await withSharingConsent();
    await c.createProposal();
    await c.submitProposal();
    await c.refreshUnderwriting();
    await c.refreshUnderwriting();
    await c.issuePaymentLink();
    await _pump(tester, c, AppRoute.payment);

    expect(find.text('SCR-16'), findsOneWidget);
    expect(find.text('Customer payment link'), findsOneWidget);
    expect(find.byType(WaitingPanel), findsOneWidget);
  });

  testWidgets('SCR-17 confirmation renders all four sold conditions',
      (tester) async {
    final c = await withSharingConsent();
    await c.createProposal();
    await c.submitProposal();
    await c.refreshUnderwriting();
    await c.refreshUnderwriting();
    await c.issuePaymentLink();
    await c.customerCompletesPayment();
    await c.issuePolicy();
    await _pump(tester, c, AppRoute.confirmation);

    expect(find.text('SCR-17'), findsOneWidget);
    expect(find.text('Policy issued by the insurer'), findsOneWidget);
    expect(find.text('Issuance confirmed by the insurer'), findsOneWidget);
    expect(find.text('Premium reconciled against the gateway'), findsOneWidget);
    expect(find.text('Record persisted in the bank audit store'), findsOneWidget);
    expect(find.text('Sold'), findsOneWidget);
  });

  testWidgets('SCR-18 funnel renders the control KPIs', (tester) async {
    final c = await signedIn();
    await _pump(tester, c, AppRoute.funnel);

    expect(find.text('SCR-18'), findsOneWidget);
    expect(find.text('KPI-03 suitability bypass rate'), findsOneWidget);
    expect(find.text('KPI-04 payments on RM device'), findsOneWidget);
  });

  testWidgets('unauthenticated access to any route is refused', (tester) async {
    final c = JourneyController.withFake(); // not signed in
    await _pump(tester, c, AppRoute.pipeline);

    expect(find.textContaining('Sign in to continue'), findsOneWidget);
  });
}
