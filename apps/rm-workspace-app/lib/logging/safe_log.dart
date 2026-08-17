/// PII-safe logging.
///
/// Implements the client-side half of INV-LOG-01 (`01-domain-model-and-invariants.md` §6.1,
/// control C5) and acceptance criterion `AC-SEC-040-1`
/// (`S03-requirements-evidence.md` §4.1):
///
/// > Given the full test suite runs, when all emitted logs are scanned for PAN,
/// > Aadhaar, full mobile, email, DOB and payment-URL patterns, then zero
/// > matches are found.
///
/// The rule this file enforces is stronger than "developers should be careful":
/// [SafeLog] is the ONLY sanctioned log sink in this application, it redacts
/// before emitting, and `test/logging/pii_redaction_test.dart` scans `lib/` to
/// assert that no `print` or `debugPrint` call exists anywhere else.
library;

import 'package:flutter/foundation.dart';

/// Regulated field patterns. Any match is redacted before a record is emitted.
///
/// Ordered most-specific first — a payment URL must be caught before the
/// generic long-digit rule turns part of it into a masked number.
final List<({RegExp pattern, String replacement})> _redactions = [
  // Payment URLs — BR-PAY-010 AC2: the URL is never written to a log.
  (
    pattern: RegExp(r'https?://[^\s"]*', caseSensitive: false),
    replacement: '[REDACTED:URL]'
  ),
  // Email
  (
    pattern: RegExp(r'[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}'),
    replacement: '[REDACTED:EMAIL]'
  ),
  // PAN — ABCDE1234F
  (
    pattern: RegExp(r'\b[A-Z]{5}[0-9]{4}[A-Z]\b'),
    replacement: '[REDACTED:PAN]'
  ),
  // Aadhaar — 12 digits, optionally spaced in groups of four
  (
    pattern: RegExp(r'\b[0-9]{4}[ -]?[0-9]{4}[ -]?[0-9]{4}\b'),
    replacement: '[REDACTED:AADHAAR]'
  ),
  // Indian mobile — 10 digits starting 6-9, with optional +91
  (
    pattern: RegExp(r'(\+?91[ -]?)?\b[6-9][0-9]{9}\b'),
    replacement: '[REDACTED:MOBILE]'
  ),
  // Dates of birth — dd/mm/yyyy, dd-mm-yyyy, yyyy-mm-dd
  (
    pattern: RegExp(r'\b(\d{2}[/-]\d{2}[/-]\d{4}|\d{4}-\d{2}-\d{2})\b'),
    replacement: '[REDACTED:DATE]'
  ),
];

/// Redacts every regulated pattern from [input].
///
/// Exposed for test. Production code should call [SafeLog.info] and friends
/// rather than redacting by hand.
String redactPii(String input) {
  var out = input;
  for (final r in _redactions) {
    out = out.replaceAll(r.pattern, r.replacement);
  }
  return out;
}

enum LogLevel { debug, info, warn, error }

/// A single emitted record. Retained in [SafeLog.records] so tests can assert
/// on what the application actually emitted.
class LogRecord {
  final LogLevel level;
  final String message;
  final DateTime at;

  const LogRecord(this.level, this.message, this.at);

  @override
  String toString() => '[${level.name.toUpperCase()}] $message';
}

/// The only sanctioned log sink in this application.
///
/// Every message passes through [redactPii] before it reaches a sink or the
/// retained buffer. There is no bypass, and no method accepts a "raw" flag.
abstract final class SafeLog {
  static final List<LogRecord> _records = [];

  /// Records emitted so far. Test-visible by design: `AC-SEC-040-1` requires
  /// scanning emitted logs, which needs the emissions to be inspectable.
  static List<LogRecord> get records => List.unmodifiable(_records);

  static void clear() => _records.clear();

  static void debug(String message) => _emit(LogLevel.debug, message);
  static void info(String message) => _emit(LogLevel.info, message);
  static void warn(String message) => _emit(LogLevel.warn, message);
  static void error(String message) => _emit(LogLevel.error, message);

  /// Audit-shaped log line. Carries identifiers and outcome only — never PII,
  /// per the domain model's event payload rule (§7).
  static void audit({
    required String action,
    required String actorId,
    String? journeyId,
    required String outcome,
  }) {
    _emit(
      LogLevel.info,
      'AUDIT action=$action actor=$actorId '
      'journey=${journeyId ?? '-'} outcome=$outcome',
    );
  }

  static void _emit(LogLevel level, String message) {
    final safe = redactPii(message);
    _records.add(LogRecord(level, safe, DateTime.now()));
    if (kDebugMode) {
      // ignore: avoid_print
      print('[${level.name.toUpperCase()}] $safe');
    }
  }
}
