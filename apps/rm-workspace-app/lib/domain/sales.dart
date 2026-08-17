/// Sales-path aggregates: customer, lead, quote, proposal, payment, policy.
///
/// State machines from `01-domain-model-and-invariants.md` §4.1, §4.4 to §4.7.
/// Business rules from `S04-product-definition-evidence.md` §4.3 (quote rules
/// QR-01 to QR-12) and `S03-requirements-evidence.md` §4 (acceptance criteria).
library;

import 'consent.dart';
import 'ids.dart';
import 'suitability.dart';

// ---------------------------------------------------------------------------
// Customer and lead
// ---------------------------------------------------------------------------

/// An ETB customer profile snapshot. The CBS record is the system of record;
/// this is a snapshot taken into the journey (relationship R-02).
///
/// Note what is absent: no full mobile, no PAN, no Aadhaar. The RM workspace
/// holds a masked mobile and nothing more, so there is no client-side value
/// for a log to leak (INV-LOG-01).
class CustomerProfile {
  final CustomerId id;
  final String cif;
  final String fullName;
  final int ageNextBirthday;
  final MaskedMobile maskedMobile;
  final List<String> etbRelationshipTypes;
  final DateTime prefillAt;

  const CustomerProfile({
    required this.id,
    required this.cif,
    required this.fullName,
    required this.ageNextBirthday,
    required this.maskedMobile,
    required this.etbRelationshipTypes,
    required this.prefillAt,
  });

  /// `AC-CUST-010-3`: a non-ETB identity is ineligible for R0.
  bool get isEtb => etbRelationshipTypes.isNotEmpty;
}

enum LeadState { newLead, assigned, contacted, qualified, converted, disqualified, expired }

class Lead {
  final LeadId id;
  final CustomerId customerId;
  final ActorId ownerActorId;
  final String ownerAgentId;
  final LeadState state;
  final DateTime createdAt;
  final DateTime lastActivityAt;

  const Lead({
    required this.id,
    required this.customerId,
    required this.ownerActorId,
    required this.ownerAgentId,
    required this.state,
    required this.createdAt,
    required this.lastActivityAt,
  });

  /// `AC-LEAD-020-2`: 90 days without activity marks the lead dormant.
  bool isDormantAt(DateTime now) =>
      now.difference(lastActivityAt) > const Duration(days: 90);
}

// ---------------------------------------------------------------------------
// Product catalogue
// ---------------------------------------------------------------------------

/// A catalogue row. Dimensions from `S04-product-definition-evidence.md` §4.2.1.
class CatalogueProduct {
  final String insurerCode;
  final String insurerDisplayName;
  final String bankProductName;
  final String insurerProductCode;
  final ProductClass productClass;
  final String insurerGroup; // 'A' or 'B'
  final int minAge;
  final int maxAge;
  final double minSumAssured;
  final double maxSumAssured;
  final bool active;

  const CatalogueProduct({
    required this.insurerCode,
    required this.insurerDisplayName,
    required this.bankProductName,
    required this.insurerProductCode,
    required this.productClass,
    required this.insurerGroup,
    required this.minAge,
    required this.maxAge,
    required this.minSumAssured,
    required this.maxSumAssured,
    this.active = true,
  });

  bool eligibleFor(int ageNextBirthday) =>
      active && ageNextBirthday >= minAge && ageNextBirthday <= maxAge;
}

// ---------------------------------------------------------------------------
// Quote
// ---------------------------------------------------------------------------

enum QuoteState {
  requested,
  inProgress,
  quoted,
  partiallyQuoted,
  failed,
  timedOut,
  selected,
  expired,
  converted,
  rejected,
}

class Offer {
  final OfferId id;
  final String insurerCode;
  final String insurerDisplayName;
  final String bankProductName;
  final double annualPremium;
  final double sumAssured;
  final int termYears;
  final String premiumFrequency;
  final DateTime validUntil;
  final List<String> keyBenefits;

  const Offer({
    required this.id,
    required this.insurerCode,
    required this.insurerDisplayName,
    required this.bankProductName,
    required this.annualPremium,
    required this.sumAssured,
    required this.termYears,
    required this.premiumFrequency,
    required this.validUntil,
    this.keyBenefits = const [],
  });

  /// QR-02 / INV-QUO-04.
  bool isValidAt(DateTime now) => now.isBefore(validUntil);

  /// INV-QUO-03: an offer with a non-positive premium or sum assured may not be
  /// selected.
  bool get isSelectable => annualPremium > 0 && sumAssured > 0;
}

class Quote {
  final QuoteId id;
  final CustomerId customerId;

  /// The proof the quote was gated on. Held so the refusal or the permission is
  /// auditable, per INV-QUO-01's requirement that a refusal produce a domain
  /// record rather than a bare 403.
  final SuitabilityEvaluationRef suitabilityRef;

  final QuoteState state;
  final List<Offer> offers;
  final List<String> failedInsurers;
  final OfferId? selectedOfferId;
  final DateTime requestedAt;

  const Quote({
    required this.id,
    required this.customerId,
    required this.suitabilityRef,
    required this.state,
    required this.requestedAt,
    this.offers = const [],
    this.failedInsurers = const [],
    this.selectedOfferId,
  });

  Offer? get selectedOffer {
    final sel = selectedOfferId;
    if (sel == null) return null;
    for (final o in offers) {
      if (o.id == sel) return o;
    }
    return null;
  }

  /// QR-06: default ordering is ascending annual premium on the normalised
  /// basis. QR-08: ties break on insurer code, deterministically.
  ///
  /// QR-07: commission is not an input, and there is no field on [Offer] that
  /// could make it one.
  List<Offer> get rankedOffers {
    final sorted = [...offers.where((o) => o.isSelectable)];
    sorted.sort((a, b) {
      final byPremium = a.annualPremium.compareTo(b.annualPremium);
      if (byPremium != 0) return byPremium;
      return a.insurerCode.compareTo(b.insurerCode);
    });
    return sorted;
  }

  /// INV-QUO-05: selection is atomic with invalidating siblings. Because
  /// [selectedOfferId] is a single field on the aggregate, two selected offers
  /// is unrepresentable rather than merely forbidden.
  Quote select(OfferId offerId, DateTime now) {
    final offer = offers.where((o) => o.id == offerId).firstOrNull;
    if (offer == null) {
      throw ArgumentError('INV-PRP-02: offer does not belong to this quote');
    }
    if (!offer.isValidAt(now)) {
      throw StateError('QUOTE_EXPIRED');
    }
    if (!offer.isSelectable) {
      throw StateError('INV-QUO-03: offer is not selectable');
    }
    return Quote(
      id: id,
      customerId: customerId,
      suitabilityRef: suitabilityRef,
      state: QuoteState.selected,
      requestedAt: requestedAt,
      offers: offers,
      failedInsurers: failedInsurers,
      selectedOfferId: offerId,
    );
  }
}

// ---------------------------------------------------------------------------
// Proposal
// ---------------------------------------------------------------------------

enum ProposalState {
  draft,
  submitted,
  underWriting,
  uwApproved,
  uwDeclined,
  awaitingPayment,
  paid,
  converted,
  issuanceFailed,
  submissionFailed,
}

class Proposal {
  final ProposalId id;
  final QuoteId quoteId;
  final OfferId offerId;
  final CustomerId customerId;

  /// The proof the submission was gated on — INV-PRP-01, control C2.
  final ConsentGrantRef sharingConsent;

  final ProposalState state;
  final Map<String, String> answers;
  final String? insurerApplicationRef;
  final DateTime? submittedAt;

  const Proposal({
    required this.id,
    required this.quoteId,
    required this.offerId,
    required this.customerId,
    required this.sharingConsent,
    required this.state,
    this.answers = const {},
    this.insurerApplicationRef,
    this.submittedAt,
  });

  Proposal copyWith({
    ProposalState? state,
    Map<String, String>? answers,
    String? insurerApplicationRef,
    DateTime? submittedAt,
  }) =>
      Proposal(
        id: id,
        quoteId: quoteId,
        offerId: offerId,
        customerId: customerId,
        sharingConsent: sharingConsent,
        state: state ?? this.state,
        answers: answers ?? this.answers,
        insurerApplicationRef: insurerApplicationRef ?? this.insurerApplicationRef,
        submittedAt: submittedAt ?? this.submittedAt,
      );

  /// INV-PRP-04: a proposal cannot be withdrawn at or after `AWAITING_PAYMENT`.
  bool get mayWithdraw =>
      state == ProposalState.draft ||
      state == ProposalState.submitted ||
      state == ProposalState.underWriting;
}

// ---------------------------------------------------------------------------
// Payment — device-isolated by construction
// ---------------------------------------------------------------------------

enum PaymentState {
  initiated,
  linkIssued,
  awaitingAuthorisation,
  authorised,
  captured,
  reconciled,
  declined,
  expired,
  uncertain,
  reconciliationBreak,
}

/// What the RM app is given when a payment is initiated.
///
/// This class is the whole of the RM app's payment vocabulary, and it is
/// deliberately incapable of carrying an instrument. There is no card number,
/// no CVV, no expiry, no account number, no UPI handle — not as optional
/// fields, not as a map, not anywhere.
///
/// INV-PAY-01 (control C4) says no API path issues a payment link into an RM
/// session and no RM principal may complete authorisation. On the client, the
/// enforcement is that there is nothing to complete: the RM holds a URL to
/// hand over and a masked mobile to read aloud.
class PaymentHandoff {
  final PaymentId paymentId;

  /// The URL the CUSTOMER opens on their OWN device. The RM app renders it as a
  /// QR code and a "sent to" confirmation. It is never logged — see
  /// `SafeLog`'s URL redaction.
  final String customerDeviceUrl;

  /// Where the link was sent — the customer's CBS-registered mobile, masked.
  final MaskedMobile sentTo;

  final DateTime expiresAt;
  final double amount;

  const PaymentHandoff({
    required this.paymentId,
    required this.customerDeviceUrl,
    required this.sentTo,
    required this.expiresAt,
    required this.amount,
  });
}

class Payment {
  final PaymentId id;
  final ProposalId proposalId;
  final PaymentState state;
  final double amount;
  final String? pgTransactionRef;

  const Payment({
    required this.id,
    required this.proposalId,
    required this.state,
    required this.amount,
    this.pgTransactionRef,
  });

  /// INV-POL-01: only a reconciled payment permits issuance. `CAPTURED` is not
  /// enough, which is what makes the four-part "sold" definition enforceable.
  bool get permitsIssuance => state == PaymentState.reconciled;
}

// ---------------------------------------------------------------------------
// Policy
// ---------------------------------------------------------------------------

enum PolicyState { pendingIssuance, issued, confirmed, active, issuanceRejected }

class Policy {
  final PolicyId id;
  final ProposalId proposalId;
  final String? policyNumber;
  final PolicyState state;
  final bool issuanceConfirmed;
  final bool paymentReconciled;
  final bool auditPersisted;

  const Policy({
    required this.id,
    required this.proposalId,
    required this.state,
    this.policyNumber,
    this.issuanceConfirmed = false,
    this.paymentReconciled = false,
    this.auditPersisted = false,
  });

  /// `AC-POL-010-2` and INV-JRN-05: "sold" is DERIVED from all four conditions
  /// and is never a settable flag. Three of four is not sold.
  bool get isSold =>
      state == PolicyState.active &&
      policyNumber != null &&
      issuanceConfirmed &&
      paymentReconciled &&
      auditPersisted;
}
