/// Repository interfaces — the seam between the RM app and the platform.
///
/// Today the only implementation is `FakeBackend`, which runs the whole R0
/// journey in memory. Real HTTP wiring to the RM Workspace BFF (bounded context
/// #2) is a later increment; swapping the implementation is the only change
/// required, because nothing outside `lib/data/` names a concrete class.
///
/// ## The guards live in these signatures
///
/// Two of the three non-negotiables are enforced by the parameter types here,
/// not by validation inside the implementations:
///
/// * [QuoteRepository.requestQuote] requires a `SuitabilityEvaluationRef`,
///   which can only be minted by a completed, unexpired, quote-permitting
///   assessment (INV-QUO-01 / SUIT-R20, control C1).
/// * [ProposalRepository.submit] requires a `ConsentGrantRef`, which can only
///   be minted by a `GRANTED`, unexpired consent (INV-PRP-01, control C2).
///
/// The third — payment device isolation, INV-PAY-01 / control C4 — is enforced
/// by what [PaymentRepository] does NOT declare. There is no method on this
/// interface that accepts an instrument, and therefore no implementation of it
/// can offer one.
library;

import '../domain/consent.dart';
import '../domain/ids.dart';
import '../domain/sales.dart';
import '../domain/suitability.dart';

class RepositoryException implements Exception {
  final String code;
  final String message;
  const RepositoryException(this.code, this.message);

  @override
  String toString() => 'RepositoryException($code): $message';
}

abstract interface class CustomerRepository {
  /// `AC-CUST-010-1`: search by CIF, registered mobile or PAN.
  Future<List<CustomerProfile>> search(String term);

  /// `AC-CUST-020-2`: prefill requires a `CNS-DP` grant. The type makes it
  /// impossible to call without one.
  Future<CustomerProfile> prefill({
    required CustomerId customerId,
    required ConsentGrantRef dataProcessingConsent,
  });
}

abstract interface class LeadRepository {
  Future<List<Lead>> listForRm(ActorId actorId);
  Future<Lead> create({required CustomerId customerId, required ActorId ownerActorId});
}

abstract interface class ConsentRepository {
  /// Dispatches an OTP to the customer's CBS-registered device (CNS-R11).
  Future<ConsentGrant> requestAndChallenge({
    required CustomerId customerId,
    required ConsentCode code,
  });

  /// Verifies the OTP the CUSTOMER entered on their OWN device.
  ///
  /// CNS-R13: there is no RM-side OTP entry for a customer-device OTP. In this
  /// fake, "the customer confirmed" is simulated; in the real implementation
  /// the BFF reports verification, it does not accept a code from the RM.
  Future<ConsentGrant> confirmCustomerVerified(ConsentId id);

  Future<ConsentGrant?> find(ConsentId id);
}

abstract interface class SuitabilityRepository {
  /// SUIT-R21: an assessment cannot be started without `CNS-SOL` consent.
  Future<SuitabilityAssessment> start({
    required CustomerId customerId,
    required ConsentGrantRef solicitationConsent,
  });

  /// SUIT-R01: all twelve inputs, or no evaluation.
  Future<SuitabilityAssessment> complete({
    required SuitabilityId id,
    required SuitabilityAnswers answers,
  });
}

abstract interface class CatalogueRepository {
  /// `AC-PROD-010-1`: the suitability outcome filter runs BEFORE the
  /// eligibility filter (CAT-R02). Requiring the ref here means an unfiltered
  /// product list cannot be requested.
  Future<List<CatalogueProduct>> eligibleProducts({
    required SuitabilityEvaluationRef suitabilityRef,
    required int ageNextBirthday,
  });
}

abstract interface class QuoteRepository {
  /// **The suitability hard-gate, at the type level.**
  ///
  /// There is no overload of this method without [suitabilityRef], and
  /// `SuitabilityEvaluationRef` cannot be constructed outside
  /// `lib/domain/suitability.dart`. A caller holding no valid assessment has
  /// nothing to pass.
  Future<Quote> requestQuote({
    required CustomerId customerId,
    required SuitabilityEvaluationRef suitabilityRef,
    required String insurerProductCode,
    required double sumAssured,
    required int termYears,
    required String idempotencyKey,
  });

  Future<Quote> pollQuote(QuoteId id);
  Future<Quote> selectOffer({required QuoteId quoteId, required OfferId offerId});
}

abstract interface class ProposalRepository {
  Future<Proposal> createDraft({
    required QuoteId quoteId,
    required OfferId offerId,
    required CustomerId customerId,
    required ConsentGrantRef sharingConsent,
  });

  Future<Proposal> saveAnswers(ProposalId id, Map<String, String> answers);

  /// **The consent gate, at the type level.**
  ///
  /// INV-PRP-01: a proposal may leave `DRAFT` only with a `GRANTED`, unexpired
  /// consent covering the required purpose. [sharingConsent] is re-validated at
  /// the point of use (§5.2, "strong at the point of use"), because a token
  /// minted five minutes ago may have expired since.
  Future<Proposal> submit({
    required ProposalId id,
    required ConsentGrantRef sharingConsent,
    required String agentId,
    required DateTime now,
  });

  Future<Proposal> refreshStatus(ProposalId id);
}

abstract interface class PaymentRepository {
  /// Issues a payment link to the CUSTOMER'S OWN DEVICE.
  ///
  /// Note the parameters: a proposal and nothing else. There is deliberately no
  /// instrument, no amount override, no "collect on RM device" flag. INV-PAY-01
  /// is unrepresentable here rather than merely forbidden.
  Future<PaymentHandoff> issueCustomerDeviceLink({required ProposalId proposalId});

  /// The RM watches. The RM does not act.
  Future<Payment> pollStatus(PaymentId id);

  /// Simulates the customer completing payment on their own device. Present
  /// only in the fake; the real implementation learns this from the PG webhook
  /// via the BFF.
  Future<Payment> simulateCustomerCompletesOnOwnDevice(PaymentId id);
}

abstract interface class PolicyRepository {
  /// INV-POL-01: issuance requires a reconciled payment. Passing the [Payment]
  /// rather than an id means the caller must hold the reconciled state.
  Future<Policy> requestIssuance({required ProposalId proposalId, required Payment payment});

  Future<Policy> pollPolicy(PolicyId id);
}
