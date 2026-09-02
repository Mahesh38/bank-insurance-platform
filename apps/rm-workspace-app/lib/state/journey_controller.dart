/// The RM journey controller.
///
/// Holds the client-side projection of one journey and drives the repositories.
/// It is a `ChangeNotifier` so screens can rebuild without any third-party
/// state-management dependency — this app has no packages beyond `flutter` and
/// `flutter_test`, which keeps the S08 supply-chain surface at zero.
///
/// INV-JRN-02 is respected: this controller stores stage plus references. It
/// does not store a business decision. `isSold` is read off the [Policy]
/// aggregate, never set here.
library;

import 'package:flutter/foundation.dart';

import '../data/fake_backend.dart';
import '../data/repositories.dart';
import '../domain/consent.dart';
import '../domain/ids.dart';
import '../domain/journey.dart';
import '../domain/sales.dart';
import '../domain/suitability.dart';
import '../logging/safe_log.dart';

class JourneyController extends ChangeNotifier {
  JourneyController({
    required this.customers,
    required this.leads,
    required this.consents,
    required this.suitability,
    required this.catalogue,
    required this.quotes,
    required this.proposals,
    required this.payments,
    required this.policies,
    DateTime Function()? clock,
  }) : _clock = clock ?? DateTime.now;

  /// Wires every repository to a single [FakeBackend] instance.
  factory JourneyController.withFake({DateTime Function()? clock}) {
    final fake = FakeBackend(clock: clock);
    return JourneyController(
      customers: fake,
      leads: fake,
      consents: fake,
      suitability: fake,
      catalogue: fake,
      quotes: fake,
      proposals: fake,
      payments: fake,
      policies: fake,
      clock: clock,
    );
  }

  final CustomerRepository customers;
  final LeadRepository leads;
  final ConsentRepository consents;
  final SuitabilityRepository suitability;
  final CatalogueRepository catalogue;
  final QuoteRepository quotes;
  final ProposalRepository proposals;
  final PaymentRepository payments;
  final PolicyRepository policies;
  final DateTime Function() _clock;

  DateTime get now => _clock();

  // --- journey state --------------------------------------------------------

  JourneyStage _stage = JourneyStage.initiated;
  JourneyStage get stage => _stage;

  bool _signedIn = false;
  bool get signedIn => _signedIn;

  CustomerProfile? customer;
  Lead? lead;
  final Map<ConsentCode, ConsentGrant> consentGrants = {};
  SuitabilityAssessment? assessment;
  List<CatalogueProduct> eligibleProducts = const [];
  CatalogueProduct? selectedProduct;
  Quote? quote;
  Proposal? proposal;
  PaymentHandoff? paymentHandoff;
  Payment? payment;
  Policy? policy;

  String? lastError;
  bool busy = false;

  // --- guard-relevant projections ------------------------------------------

  /// The capability token for the quote path, or `null`.
  ///
  /// This getter is the ONLY route by which a screen can obtain a
  /// [SuitabilityEvaluationRef], and it delegates to
  /// `SuitabilityAssessment.evaluationRef`, which applies all four SUIT-R20
  /// conditions.
  SuitabilityEvaluationRef? suitabilityRefFor(ProductClass c) =>
      assessment?.evaluationRef(c, now);

  /// Convenience for R0, which is Term only.
  SuitabilityEvaluationRef? get termSuitabilityRef =>
      suitabilityRefFor(ProductClass.term);

  ConsentGrantRef? consentRef(ConsentCode code) =>
      consentGrants[code]?.grantRef(now);

  bool get hasQuotePermission => termSuitabilityRef != null;
  bool get hasSharingConsent => consentRef(ConsentCode.sharing) != null;

  // --- transitions ----------------------------------------------------------

  void _advance(JourneyStage to) {
    if (!isLegalTransition(_stage, to)) {
      // INV-JRN-01: unknown transitions are rejected, not silently applied.
      SafeLog.warn('ILLEGAL_TRANSITION from=${_stage.name} to=${to.name}');
      throw StateError('INV-JRN-01: illegal transition ${_stage.name} -> ${to.name}');
    }
    _stage = to;
  }

  Future<void> _run(Future<void> Function() body) async {
    busy = true;
    lastError = null;
    notifyListeners();
    try {
      await body();
    } on RepositoryException catch (e) {
      lastError = '${e.code}: ${e.message}';
    } on StateError catch (e) {
      lastError = e.message;
    } finally {
      busy = false;
      notifyListeners();
    }
  }

  void signIn() {
    _signedIn = true;
    SafeLog.audit(action: 'RM_SIGNED_IN', actorId: kDemoActor.value, outcome: 'OK');
    notifyListeners();
  }

  Future<List<CustomerProfile>> searchCustomers(String term) =>
      customers.search(term);

  Future<void> selectCustomer(CustomerProfile c) async => _run(() async {
        customer = c;
        notifyListeners();
      });

  Future<void> createLead() async => _run(() async {
        final c = customer!;
        lead = await leads.create(customerId: c.id, ownerActorId: kDemoActor);
        _advance(JourneyStage.needAnalysis);
      });

  /// Requests all three R0 consents and dispatches OTPs to the customer device.
  Future<void> requestConsents() async => _run(() async {
        for (final code in [
          ConsentCode.dataProcessing,
          ConsentCode.solicitation,
          ConsentCode.communications,
        ]) {
          consentGrants[code] =
              await consents.requestAndChallenge(customerId: customer!.id, code: code);
        }
      });

  /// The CUSTOMER verified on their OWN device; the platform reports it.
  ///
  /// CNS-R13: there is no parameter here for an OTP code, because the RM does
  /// not enter one.
  Future<void> customerVerifiedConsents() async => _run(() async {
        for (final code in consentGrants.keys.toList()) {
          consentGrants[code] =
              await consents.confirmCustomerVerified(consentGrants[code]!.id);
        }
      });

  Future<void> startSuitability() async => _run(() async {
        final solicitation = consentRef(ConsentCode.solicitation);
        if (solicitation == null) {
          throw const RepositoryException(
              'CONSENT_REQUIRED', 'SUIT-R21: CNS-SOL consent is required');
        }
        assessment = await suitability.start(
          customerId: customer!.id,
          solicitationConsent: solicitation,
        );
      });

  Future<void> completeSuitability(SuitabilityAnswers answers) async => _run(() async {
        assessment = await suitability.complete(id: assessment!.id, answers: answers);
        if (_stage == JourneyStage.needAnalysis) {
          _advance(JourneyStage.suitabilityComplete);
        }
      });

  /// Requires the capability token — the compiler will not let this be called
  /// without a completed, quote-permitting assessment.
  Future<void> loadEligibleProducts() async => _run(() async {
        final ref = termSuitabilityRef;
        if (ref == null) {
          throw const RepositoryException(
              'SUITABILITY_EVALUATION_REQUIRED', 'SUIT-R20: no valid evaluation');
        }
        eligibleProducts = await catalogue.eligibleProducts(
          suitabilityRef: ref,
          ageNextBirthday: customer!.ageNextBirthday,
        );
        if (_stage == JourneyStage.suitabilityComplete) {
          _advance(JourneyStage.consentCaptured);
        }
      });

  Future<void> requestQuote({
    required CatalogueProduct product,
    required double sumAssured,
    required int termYears,
  }) async =>
      _run(() async {
        final ref = termSuitabilityRef;
        if (ref == null) {
          throw const RepositoryException(
              'SUITABILITY_EVALUATION_REQUIRED', 'SUIT-R20: no valid evaluation');
        }
        selectedProduct = product;
        if (_stage == JourneyStage.consentCaptured) _advance(JourneyStage.quoting);
        quote = await quotes.requestQuote(
          customerId: customer!.id,
          suitabilityRef: ref,
          insurerProductCode: product.insurerProductCode,
          sumAssured: sumAssured,
          termYears: termYears,
          idempotencyKey: 'idem-${lead!.id.value}-${product.insurerProductCode}',
        );
      });

  Future<void> selectOffer(OfferId offerId) async => _run(() async {
        quote = await quotes.selectOffer(quoteId: quote!.id, offerId: offerId);
        _advance(JourneyStage.quoteSelected);
      });

  Future<void> createProposal() async => _run(() async {
        final sharing = consentRef(ConsentCode.sharing);
        if (sharing == null) {
          throw const RepositoryException(
              'CONSENT_REQUIRED', 'INV-PRP-01: CNS-SHR consent is required');
        }
        proposal = await proposals.createDraft(
          quoteId: quote!.id,
          offerId: quote!.selectedOfferId!,
          customerId: customer!.id,
          sharingConsent: sharing,
        );
        _advance(JourneyStage.proposalInProgress);
      });

  /// Captures the insurer-sharing consent (CNS-SHR), which is always a separate
  /// interaction because the counterparty is named in the statement.
  Future<void> captureSharingConsent() async => _run(() async {
        final grant = await consents.requestAndChallenge(
            customerId: customer!.id, code: ConsentCode.sharing);
        consentGrants[ConsentCode.sharing] =
            await consents.confirmCustomerVerified(grant.id);
      });

  Future<void> saveProposalAnswers(Map<String, String> answers) async =>
      _run(() async {
        proposal = await proposals.saveAnswers(proposal!.id, answers);
      });

  Future<void> submitProposal() async => _run(() async {
        final sharing = consentRef(ConsentCode.sharing);
        if (sharing == null) {
          throw const RepositoryException(
              'CONSENT_REQUIRED', 'INV-PRP-01: sharing consent is absent or expired');
        }
        proposal = await proposals.submit(
          id: proposal!.id,
          sharingConsent: sharing,
          agentId: kDemoAgentId,
          now: now,
        );
        _advance(JourneyStage.underwriting);
      });

  Future<void> refreshUnderwriting() async => _run(() async {
        proposal = await proposals.refreshStatus(proposal!.id);
        if (proposal!.state == ProposalState.awaitingPayment &&
            _stage == JourneyStage.underwriting) {
          _advance(JourneyStage.paymentPending);
        }
      });

  Future<void> issuePaymentLink() async => _run(() async {
        paymentHandoff = await payments.issueCustomerDeviceLink(proposalId: proposal!.id);
        payment = await payments.pollStatus(paymentHandoff!.paymentId);
      });

  /// Simulates the customer paying on their own device.
  Future<void> customerCompletesPayment() async => _run(() async {
        payment = await payments
            .simulateCustomerCompletesOnOwnDevice(paymentHandoff!.paymentId);
        _advance(JourneyStage.paymentSettled);
      });

  Future<void> issuePolicy() async => _run(() async {
        // INV-POL-01 is checked BEFORE the saga advances.
        //
        // Advancing first and letting the repository refuse afterwards leaves
        // the journey sitting in `issuancePending` for a payment that was never
        // reconciled — a stage that asserts something untrue. The precondition
        // owns the ordering, not the happy path.
        final p = payment;
        if (p == null || !p.permitsIssuance) {
          throw const RepositoryException('PAYMENT_NOT_RECONCILED',
              'INV-POL-01: issuance requires a reconciled payment');
        }
        _advance(JourneyStage.issuancePending);
        policy = await policies.requestIssuance(
          proposalId: proposal!.id,
          payment: p,
        );
        _advance(JourneyStage.issued);
        if (policy!.isSold) _advance(JourneyStage.sold);
      });

  /// Resets to a clean journey — used by the pipeline screen's "new journey".
  void reset() {
    _stage = JourneyStage.initiated;
    customer = null;
    lead = null;
    consentGrants.clear();
    assessment = null;
    eligibleProducts = const [];
    selectedProduct = null;
    quote = null;
    proposal = null;
    paymentHandoff = null;
    payment = null;
    policy = null;
    lastError = null;
    notifyListeners();
  }
}
