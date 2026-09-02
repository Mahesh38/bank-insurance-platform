/// Consent — a grant with immutable evidence.
///
/// State machine from `01-domain-model-and-invariants.md` §4.2. Rules from
/// `docs/au-bank-insurance-platform/rule-packs/consent-rule-pack.md`.
///
/// ## The structural guard
///
/// [ConsentGrantRef] has a library-private constructor. The ONLY way to obtain
/// one is [ConsentGrant.grantRef], which returns `null` unless the grant is
/// `GRANTED` and unexpired. Downstream code that requires proof of consent
/// takes a `ConsentGrantRef` parameter, so "submit a proposal without consent"
/// is not a runtime check that can be forgotten — it does not compile.
///
/// This implements INV-PRP-01 (control C2, non-waivable) at the type level.
library;

import 'ids.dart';

/// Consent events in R0. Codes from consent-rule-pack §2.
enum ConsentCode {
  /// Data processing and CBS/CIF prefill.
  dataProcessing('CNS-DP', 'Data processing and account-details prefill'),

  /// Insurance solicitation, need analysis and suitability.
  solicitation('CNS-SOL', 'Insurance need analysis and suitability assessment'),

  /// Data sharing with the selected insurer.
  sharing('CNS-SHR', 'Sharing your details with the selected insurer'),

  /// Servicing communications.
  communications('CNS-COM', 'Policy servicing messages');

  final String code;
  final String shortLabel;
  const ConsentCode(this.code, this.shortLabel);
}

/// Consent aggregate states — §4.2. `GRANTED` is the only state that satisfies
/// a downstream precondition.
enum ConsentState { requested, otpPending, granted, withdrawn, expired, abandoned }

/// Validity windows from consent-rule-pack §6 (CNS-R21 to CNS-R23).
Duration validityFor(ConsentCode code) => switch (code) {
      ConsentCode.dataProcessing => const Duration(days: 90),
      ConsentCode.solicitation => const Duration(days: 90),
      ConsentCode.sharing => const Duration(days: 30),
      // CNS-R23: no expiry; persists until withdrawn.
      ConsentCode.communications => const Duration(days: 36500),
    };

/// Proof that a valid consent grant exists.
///
/// Cannot be constructed outside this library. See the library doc comment.
class ConsentGrantRef {
  final ConsentId consentId;
  final ConsentCode code;
  final CustomerId customerId;
  final DateTime validUntil;

  const ConsentGrantRef._({
    required this.consentId,
    required this.code,
    required this.customerId,
    required this.validUntil,
  });

  bool isValidAt(DateTime now) => now.isBefore(validUntil);

  @override
  String toString() => 'ConsentGrantRef(${code.code}, ${consentId.value})';
}

/// The consent evidence record. Append-only by construction: every field is
/// final and there is no mutator. CNS-R16 to CNS-R18.
class ConsentEvidence {
  final String statementText;
  final String statementVersion;
  final String statementTextHash;
  final String otpTransactionId;
  final DateTime otpVerifiedAt;
  final DateTime capturedAt;
  final ActorId capturedByActorId;
  final String agentId;

  const ConsentEvidence({
    required this.statementText,
    required this.statementVersion,
    required this.statementTextHash,
    required this.otpTransactionId,
    required this.otpVerifiedAt,
    required this.capturedAt,
    required this.capturedByActorId,
    required this.agentId,
  });
}

/// A consent grant.
class ConsentGrant {
  final ConsentId id;
  final ConsentCode code;
  final CustomerId customerId;
  final ConsentState state;
  final DateTime? grantedAt;
  final DateTime? validUntil;
  final ConsentEvidence? evidence;

  const ConsentGrant({
    required this.id,
    required this.code,
    required this.customerId,
    required this.state,
    this.grantedAt,
    this.validUntil,
    this.evidence,
  });

  /// Opens a consent request. State machine entry point.
  factory ConsentGrant.request({
    required ConsentId id,
    required ConsentCode code,
    required CustomerId customerId,
  }) =>
      ConsentGrant(
        id: id,
        code: code,
        customerId: customerId,
        state: ConsentState.requested,
      );

  /// `REQUESTED -> OTP_PENDING`. An OTP has been dispatched to the customer's
  /// registered device (CNS-R11).
  ConsentGrant challenge() {
    if (state != ConsentState.requested) {
      throw StateError('Illegal transition: $state -> otpPending');
    }
    return _copy(state: ConsentState.otpPending);
  }

  /// `OTP_PENDING -> GRANTED`.
  ///
  /// INV-CNS-02: a grant may reach `GRANTED` only from `OTP_PENDING` with a
  /// verified OTP transaction id. CNS-R10: no verified OTP, no consent.
  ConsentGrant verifyOtp({
    required String otpTransactionId,
    required DateTime now,
    required ConsentEvidence evidence,
  }) {
    if (state != ConsentState.otpPending) {
      throw StateError('Illegal transition: $state -> granted');
    }
    if (otpTransactionId.isEmpty) {
      throw ArgumentError('CNS-R10: consent requires a verified OTP transaction');
    }
    return _copy(
      state: ConsentState.granted,
      grantedAt: now,
      validUntil: now.add(validityFor(code)),
      evidence: evidence,
    );
  }

  /// `GRANTED -> WITHDRAWN`. CNS-R27 to CNS-R32: withdrawal never deletes the
  /// prior evidence, so [evidence] is carried forward unchanged.
  ConsentGrant withdraw() {
    if (state != ConsentState.granted) {
      throw StateError('Illegal transition: $state -> withdrawn');
    }
    return _copy(state: ConsentState.withdrawn);
  }

  /// The capability token, or `null` if this grant does not prove anything.
  ///
  /// This is the single mint point for [ConsentGrantRef] in the entire
  /// application.
  ConsentGrantRef? grantRef(DateTime now) {
    if (state != ConsentState.granted) return null;
    final until = validUntil;
    if (until == null || !now.isBefore(until)) return null;
    return ConsentGrantRef._(
      consentId: id,
      code: code,
      customerId: customerId,
      validUntil: until,
    );
  }

  ConsentGrant _copy({
    ConsentState? state,
    DateTime? grantedAt,
    DateTime? validUntil,
    ConsentEvidence? evidence,
  }) =>
      ConsentGrant(
        id: id,
        code: code,
        customerId: customerId,
        state: state ?? this.state,
        grantedAt: grantedAt ?? this.grantedAt,
        validUntil: validUntil ?? this.validUntil,
        evidence: evidence ?? this.evidence,
      );
}
