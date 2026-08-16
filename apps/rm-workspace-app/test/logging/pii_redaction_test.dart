/// **No PII in logs.**
///
/// INV-LOG-01 (control C5) and `AC-SEC-040-1`:
///
/// > Given the full test suite runs, when all emitted logs are scanned for PAN,
/// > Aadhaar, full mobile, email, DOB and payment-URL patterns, then zero
/// > matches are found.
///
/// Three assertions: the redactor works; the application's own emissions during
/// a complete journey contain nothing regulated; and no unredacted log sink
/// exists anywhere in `lib/`.
library;

import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:rm_workspace_app/logging/safe_log.dart';

import '../guards/payment_device_isolation_test.dart' show stripDartComments;
import '../support/journey_fixtures.dart';

/// The patterns AC-SEC-040-1 names. Applied to emitted log text.
final _regulatedPatterns = <String, RegExp>{
  'PAN': RegExp(r'\b[A-Z]{5}[0-9]{4}[A-Z]\b'),
  'Aadhaar': RegExp(r'\b[0-9]{4}[ -]?[0-9]{4}[ -]?[0-9]{4}\b'),
  'mobile': RegExp(r'(\+?91[ -]?)?\b[6-9][0-9]{9}\b'),
  'email': RegExp(r'[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}'),
  'DOB': RegExp(r'\b(\d{2}[/-]\d{2}[/-]\d{4})\b'),
  'URL': RegExp(r'https?://'),
};

void main() {
  setUp(SafeLog.clear);

  group('the redactor removes every regulated pattern', () {
    test('PAN', () {
      expect(redactPii('customer pan ABCDE1234F noted'),
          'customer pan [REDACTED:PAN] noted');
    });

    test('Aadhaar, spaced and unspaced', () {
      expect(redactPii('uid 123456781234'), contains('[REDACTED:AADHAAR]'));
      expect(redactPii('uid 1234 5678 1234'), contains('[REDACTED:AADHAAR]'));
    });

    test('Indian mobile, with and without country code', () {
      expect(redactPii('call 9876543210'), contains('[REDACTED:MOBILE]'));
      expect(redactPii('call +91 9876543210'), contains('[REDACTED:MOBILE]'));
    });

    test('email', () {
      expect(redactPii('meera@example.com'), '[REDACTED:EMAIL]');
    });

    test('date of birth', () {
      expect(redactPii('dob 14/03/1988'), contains('[REDACTED:DATE]'));
      expect(redactPii('dob 1988-03-14'), contains('[REDACTED:DATE]'));
    });

    test('payment URL — BR-PAY-010 AC2', () {
      final out = redactPii(
          'link https://pg.example-aubank.invalid/pay/PAY-000001 issued');
      expect(out, contains('[REDACTED:URL]'));
      expect(out, isNot(contains('pg.example-aubank')));
    });

    test('a realistic mixed record is fully scrubbed', () {
      final out = redactPii(
        'proposal for ABCDE1234F, mobile 9876543210, meera@example.com, '
        'dob 14/03/1988, pay at https://pg.invalid/x',
      );
      for (final entry in _regulatedPatterns.entries) {
        expect(entry.value.hasMatch(out), isFalse,
            reason: '${entry.key} survived redaction in: $out');
      }
    });
  });

  group('the application emits nothing regulated during a full journey', () {
    test('walk the complete R0 journey, then scan every emitted record',
        () async {
      final c = await withSharingConsent();
      await c.createProposal();
      await c.saveProposalAnswers({'nomineeName': 'A Nominee'});
      await c.submitProposal();
      await c.refreshUnderwriting();
      await c.refreshUnderwriting();
      await c.issuePaymentLink();
      await c.customerCompletesPayment();
      await c.issuePolicy();

      expect(SafeLog.records, isNotEmpty,
          reason: 'the journey must actually have logged something');

      for (final record in SafeLog.records) {
        for (final entry in _regulatedPatterns.entries) {
          expect(
            entry.value.hasMatch(record.message),
            isFalse,
            reason: '${entry.key} pattern found in emitted log: '
                '"${record.message}"',
          );
        }
      }
    });

    test('the payment URL never reaches a log record', () async {
      final c = await withSharingConsent();
      await c.createProposal();
      await c.submitProposal();
      await c.refreshUnderwriting();
      await c.refreshUnderwriting();
      await c.issuePaymentLink();

      final url = c.paymentHandoff!.customerDeviceUrl;
      expect(url, isNotEmpty);

      for (final record in SafeLog.records) {
        expect(record.message.contains(url), isFalse,
            reason: 'the payment URL must never appear in a log');
      }
    });

    test('audit records carry identifiers and outcome only', () async {
      SafeLog.audit(
        action: 'PROPOSAL_SUBMITTED',
        actorId: 'RM-DEMO',
        journeyId: 'JRN-1',
        outcome: 'OK',
      );

      final record = SafeLog.records.single;
      expect(record.message, contains('action=PROPOSAL_SUBMITTED'));
      expect(record.message, contains('actor=RM-DEMO'));
      expect(record.message, contains('outcome=OK'));
    });
  });

  group('SafeLog is the only log sink', () {
    test('no print or debugPrint call exists outside the logging library', () {
      final offences = <String>[];

      for (final entity in Directory('lib').listSync(recursive: true)) {
        if (entity is! File || !entity.path.endsWith('.dart')) continue;
        if (entity.path.endsWith('logging/safe_log.dart')) continue;

        final source = stripDartComments(entity.readAsStringSync());
        for (final call in const ['print(', 'debugPrint(', 'log(']) {
          if (source.contains(call)) {
            offences.add('${entity.path}: $call');
          }
        }
      }

      expect(offences, isEmpty,
          reason: 'INV-LOG-01: SafeLog is the only sanctioned sink. '
              'Found:\n${offences.join('\n')}');
    });

    test('SafeLog exposes no method that bypasses redaction', () {
      final source =
          stripDartComments(File('lib/logging/safe_log.dart').readAsStringSync());
      // Every emission path funnels through _emit, which calls redactPii.
      expect(source.contains('_emit(LogLevel'), isTrue);
      expect(source.contains('redactPii(message)'), isTrue);
      // No "raw" escape hatch.
      expect(source.toLowerCase().contains('raw:'), isFalse);
      expect(source.toLowerCase().contains('skipredaction'), isFalse);
    });
  });
}
