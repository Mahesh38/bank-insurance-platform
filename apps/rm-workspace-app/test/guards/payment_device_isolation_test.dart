/// **Payment device isolation.**
///
/// INV-PAY-01, control C4, non-waivable. RBI cyber-security guidance prohibits
/// accepting insurance premium payment on a bank employee's device;
/// `AC-PAY-010-2` requires the RM surface to render no payment form and no card
/// or UPI input at any point.
///
/// This control is proved by ASSERTING AN ABSENCE, which needs two different
/// kinds of test:
///
/// * a **widget** test — the rendered payment screen contains no text input in
///   any of its states;
/// * a **source** test — no payment-instrument identifier appears anywhere
///   under `lib/`, so no future screen can acquire one either.
///
/// The widget test alone would pass a screen that hides its card field behind a
/// flag. The source test alone would pass a screen that collects a card number
/// in a field called `x`. Together they are hard to defeat by accident.
library;

import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:rm_workspace_app/app.dart';
import 'package:rm_workspace_app/domain/sales.dart';
import 'package:rm_workspace_app/guards/journey_guard.dart';
import 'package:rm_workspace_app/screens/payment_screen.dart';

import '../support/journey_fixtures.dart';

/// Identifiers that would indicate the RM app is capable of taking a payment.
/// Matched case-insensitively against the source of `lib/`.
/// Strips comments before scanning.
///
/// This matters: the payment screen's own documentation explains that it holds
/// no CVV and no card number, and a naive scan would flag the very comment that
/// records the control. What is being asserted is the absence of a *capability*,
/// not the absence of the words.
String stripDartComments(String source) {
  final withoutBlocks = source.replaceAll(RegExp(r'/\*.*?\*/', dotAll: true), '');
  return withoutBlocks
      .split('\n')
      .where((line) {
        final t = line.trimLeft();
        return !t.startsWith('//');
      })
      .join('\n');
}

const _forbiddenInstrumentTerms = <String>[
  'cardnumber',
  'card_number',
  'cardno',
  'cvv',
  'cvc',
  'expirymonth',
  'expiryyear',
  'nameoncard',
  'accountnumber',
  'account_number',
  'ifsc',
  'upiid',
  'upi_id',
  'vpa',
  'netbanking',
  'pan_entry',
];

Future<PaymentHandoffScreen?> _pumpPaymentScreen(WidgetTester tester,
    {required bool afterLinkIssued, required bool afterPaid}) async {
  final c = await withSharingConsent();
  await c.createProposal();
  await c.submitProposal();
  await c.refreshUnderwriting(); // -> uwApproved
  await c.refreshUnderwriting(); // -> awaitingPayment

  if (afterLinkIssued) await c.issuePaymentLink();
  if (afterPaid) await c.customerCompletesPayment();

  await tester.pumpWidget(
    RmWorkspaceApp(controller: c, initialRoute: AppRoute.payment),
  );
  await tester.pumpAndSettle();
  return null;
}

void main() {
  group('the RM payment screen has no input surface, in any state', () {
    testWidgets('before the link is issued', (tester) async {
      await _pumpPaymentScreen(tester, afterLinkIssued: false, afterPaid: false);

      expect(find.byType(PaymentHandoffScreen), findsOneWidget);
      expect(find.byType(TextField), findsNothing);
      expect(find.byType(TextFormField), findsNothing);
      expect(find.byType(EditableText), findsNothing);
    });

    testWidgets('while waiting for the customer to pay', (tester) async {
      await _pumpPaymentScreen(tester, afterLinkIssued: true, afterPaid: false);

      expect(find.byType(PaymentHandoffScreen), findsOneWidget);
      expect(find.byType(TextField), findsNothing);
      expect(find.byType(TextFormField), findsNothing);

      // The waiting state exists and tells the RM what to do — which is
      // nothing. S05 §4.5: dead air is what makes an RM reach for the
      // customer's phone.
      expect(find.textContaining('own device'), findsWidgets);
      expect(find.textContaining('nothing for you to enter'), findsOneWidget);
    });

    testWidgets('after payment is reconciled', (tester) async {
      await _pumpPaymentScreen(tester, afterLinkIssued: true, afterPaid: true);

      expect(find.byType(TextField), findsNothing);
      expect(find.byType(TextFormField), findsNothing);
      expect(find.textContaining('reconciled'), findsWidgets);
    });

    testWidgets('the screen states the prohibition to the RM', (tester) async {
      await _pumpPaymentScreen(tester, afterLinkIssued: true, afterPaid: false);

      expect(
        find.textContaining('Do not take the customer\'s phone'),
        findsOneWidget,
      );
    });
  });

  group('no payment-instrument capability exists anywhere in lib/', () {
    test('source scan finds no card, account or UPI identifiers', () {
      final libDir = Directory('lib');
      expect(libDir.existsSync(), isTrue,
          reason: 'test must run from the package root');

      final offences = <String>[];

      for (final entity in libDir.listSync(recursive: true)) {
        if (entity is! File || !entity.path.endsWith('.dart')) continue;
        final source =
            stripDartComments(entity.readAsStringSync()).toLowerCase();
        for (final term in _forbiddenInstrumentTerms) {
          if (source.contains(term)) {
            offences.add('${entity.path}: "$term"');
          }
        }
      }

      expect(
        offences,
        isEmpty,
        reason: 'INV-PAY-01 / AC-PAY-010-2: the RM application must have no '
            'payment-instrument capability. Found:\n${offences.join('\n')}',
      );
    });

    test('PaymentRepository declares no method that could accept an instrument',
        () {
      final source =
          stripDartComments(File('lib/data/repositories.dart').readAsStringSync());
      final paymentSection = source.substring(
        source.indexOf('abstract interface class PaymentRepository'),
      );
      final body = paymentSection.substring(
        0,
        paymentSection.indexOf('abstract interface class PolicyRepository'),
      );

      // The only inputs the payment interface accepts are identifiers.
      expect(body.contains('ProposalId'), isTrue);
      expect(body.contains('PaymentId'), isTrue);

      for (final term in _forbiddenInstrumentTerms) {
        expect(body.toLowerCase().contains(term), isFalse,
            reason: 'PaymentRepository must not name "$term"');
      }

      // And it exposes exactly the three operations R0 needs: issue a link to
      // the customer device, watch the status, and (in the fake) observe the
      // customer completing it.
      expect(body.contains('issueCustomerDeviceLink'), isTrue);
      expect(body.contains('pollStatus'), isTrue);
    });
  });

  group('the handoff object cannot carry an instrument', () {
    test('PaymentHandoff exposes only identifiers, destination and amount',
        () async {
      final c = await withSharingConsent();
      await c.createProposal();
      await c.submitProposal();
      await c.refreshUnderwriting();
      await c.refreshUnderwriting();
      await c.issuePaymentLink();

      final h = c.paymentHandoff!;
      expect(h.paymentId, isNotNull);
      expect(h.customerDeviceUrl, startsWith('https://'));
      expect(h.sentTo.lastTwo, isNotEmpty);
      expect(h.amount, greaterThan(0));

      // The destination is masked — the RM app never holds the full number.
      expect(h.sentTo.toString(), contains('••••'));
    });

    test('INV-POL-01 — issuance is refused unless payment is RECONCILED',
        () async {
      final c = await withSharingConsent();
      await c.createProposal();
      await c.submitProposal();
      await c.refreshUnderwriting();
      await c.refreshUnderwriting();
      await c.issuePaymentLink();

      // The payment is LINK_ISSUED, not reconciled.
      expect(c.payment!.state, PaymentState.linkIssued);
      expect(c.payment!.permitsIssuance, isFalse);

      await c.issuePolicy();
      expect(c.policy, isNull);
      expect(c.lastError, contains('PAYMENT_NOT_RECONCILED'));
    });
  });
}
