/// In-memory implementation of every repository.
///
/// This makes the R0 assisted journey runnable end to end **today**, with no
/// backend. It is a demonstration and test substrate, not evidence that the
/// platform works — the bounded contexts behind these interfaces do not exist
/// yet, and `S11` is not proven by anything in this file.
///
/// Where the real system would call the RM Workspace BFF, this class returns a
/// deterministic in-memory result. Determinism is deliberate: the widget tests
/// depend on it, and `SUIT-ALGO-LIFE-v1.0` is required to be reproducible
/// (SUIT-R05) so a fake that randomised would be lying about the model.
library;

import '../domain/consent.dart';
import '../domain/ids.dart';
import '../domain/journey.dart';
import '../domain/sales.dart';
import '../domain/suitability.dart';
import '../logging/safe_log.dart';
import 'repositories.dart';

/// R0 product matrix content.
///
/// `S04-product-definition-evidence.md` §4.2.2 records the R0 population rule
/// as one Group A / Life / Term slot, and records the actual insurer and
/// product as **UNKNOWN pending Bancassurance confirmation (S04-OPEN-01)**.
/// These rows are therefore explicitly labelled sample data. They are not a
/// claim about AU Bank's commercial panel.
const List<CatalogueProduct> kSampleR0Catalogue = [
  CatalogueProduct(
    insurerCode: 'SAMPLE-A1',
    insurerDisplayName: 'Sample Insurer A (placeholder — S04-OPEN-01)',
    bankProductName: 'Term Protect (sample)',
    insurerProductCode: 'SMPL-TERM-01',
    productClass: ProductClass.term,
    insurerGroup: 'A',
    minAge: 18,
    maxAge: 65,
    minSumAssured: 500000,
    maxSumAssured: 100000000,
  ),
  CatalogueProduct(
    insurerCode: 'SAMPLE-A2',
    insurerDisplayName: 'Sample Insurer B (placeholder — S04-OPEN-01)',
    bankProductName: 'Life Secure Term (sample)',
    insurerProductCode: 'SMPL-TERM-02',
    productClass: ProductClass.term,
    insurerGroup: 'A',
    minAge: 18,
    maxAge: 60,
    minSumAssured: 1000000,
    maxSumAssured: 50000000,
  ),
];

class FakeBackend
    implements
        CustomerRepository,
        LeadRepository,
        ConsentRepository,
        SuitabilityRepository,
        CatalogueRepository,
        QuoteRepository,
        ProposalRepository,
        PaymentRepository,
        PolicyRepository {
  FakeBackend({DateTime Function()? clock}) : _clock = clock ?? DateTime.now;

  final DateTime Function() _clock;
  int _seq = 0;

  String _id(String prefix) => '$prefix-${(++_seq).toString().padLeft(6, '0')}';

  final Map<String, ConsentGrant> _consents = {};
  final Map<String, SuitabilityAssessment> _assessments = {};
  final Map<String, Quote> _quotes = {};
  final Map<String, Proposal> _proposals = {};
  final Map<String, Payment> _payments = {};
  final Map<String, Policy> _policies = {};
  final Map<String, String> _idempotency = {};

  /// Sample ETB customers. Masked mobile only — the RM app never holds a full
  /// number, so there is no PII here to leak.
  late final List<CustomerProfile> _customers = [
    CustomerProfile(
      id: const CustomerId('CUST-000001'),
      cif: 'AU10029384',
      fullName: 'Meera Krishnan',
      ageNextBirthday: 38,
      maskedMobile: const MaskedMobile('42'),
      etbRelationshipTypes: const ['SAVINGS', 'FASTAG'],
      prefillAt: _clock(),
    ),
    CustomerProfile(
      id: const CustomerId('CUST-000002'),
      cif: 'AU10044517',
      fullName: 'Rohit Sanghvi',
      ageNextBirthday: 29,
      maskedMobile: const MaskedMobile('07'),
      etbRelationshipTypes: const ['CURRENT', 'LOAN'],
      prefillAt: _clock(),
    ),
    CustomerProfile(
      id: const CustomerId('CUST-000003'),
      cif: 'AU10077231',
      fullName: 'Ananya Deshpande',
      ageNextBirthday: 52,
      maskedMobile: const MaskedMobile('88'),
      etbRelationshipTypes: const ['FIXED_DEPOSIT'],
      prefillAt: _clock(),
    ),
  ];

  // -------------------------------------------------------------------------
  // Customer
  // -------------------------------------------------------------------------

  @override
  Future<List<CustomerProfile>> search(String term) async {
    final t = term.trim().toLowerCase();
    if (t.isEmpty) return const [];
    // AC-SEC-030-1: the search is audited. Note the payload — identifiers and
    // outcome, never the search term, which may itself be a mobile or a PAN.
    SafeLog.audit(
        action: 'CUSTOMER_SEARCH', actorId: 'RM-DEMO', outcome: 'EXECUTED');
    return _customers
        .where((c) =>
            c.cif.toLowerCase().contains(t) ||
            c.fullName.toLowerCase().contains(t) ||
            c.maskedMobile.lastTwo == t)
        .toList();
  }

  @override
  Future<CustomerProfile> prefill({
    required CustomerId customerId,
    required ConsentGrantRef dataProcessingConsent,
  }) async {
    if (!dataProcessingConsent.isValidAt(_clock())) {
      throw const RepositoryException('CONSENT_EXPIRED', 'CNS-R21: consent has expired');
    }
    return _customers.firstWhere((c) => c.id == customerId);
  }

  // -------------------------------------------------------------------------
  // Lead
  // -------------------------------------------------------------------------

  final List<Lead> _leads = [];

  @override
  Future<List<Lead>> listForRm(ActorId actorId) async =>
      _leads.where((l) => l.ownerActorId == actorId).toList();

  @override
  Future<Lead> create({
    required CustomerId customerId,
    required ActorId ownerActorId,
  }) async {
    final now = _clock();
    final lead = Lead(
      id: LeadId(_id('LEAD')),
      customerId: customerId,
      ownerActorId: ownerActorId,
      ownerAgentId: 'SP-DEMO-778812',
      state: LeadState.qualified,
      createdAt: now,
      lastActivityAt: now,
    );
    _leads.add(lead);
    SafeLog.audit(action: 'LEAD_CREATED', actorId: ownerActorId.value, outcome: 'OK');
    return lead;
  }

  // -------------------------------------------------------------------------
  // Consent
  // -------------------------------------------------------------------------

  @override
  Future<ConsentGrant> requestAndChallenge({
    required CustomerId customerId,
    required ConsentCode code,
  }) async {
    final grant = ConsentGrant.request(
      id: ConsentId(_id('CNS')),
      code: code,
      customerId: customerId,
    ).challenge();
    _consents[grant.id.value] = grant;
    SafeLog.audit(
        action: 'CONSENT_OTP_DISPATCHED', actorId: 'RM-DEMO', outcome: 'PENDING');
    return grant;
  }

  @override
  Future<ConsentGrant> confirmCustomerVerified(ConsentId id) async {
    final existing = _consents[id.value];
    if (existing == null) {
      throw const RepositoryException('CONSENT_NOT_FOUND', 'No such consent');
    }
    final now = _clock();
    final granted = existing.verifyOtp(
      otpTransactionId: _id('OTP'),
      now: now,
      evidence: ConsentEvidence(
        statementText: _statementFor(existing.code),
        statementVersion: '${existing.code.code}-en-IN-v1.0',
        // CNS-R01 requires a SHA-256 of the rendered text. The fake records a
        // deterministic placeholder; the real hash is computed server-side
        // where the statement is rendered.
        statementTextHash: 'sha256:placeholder-${existing.code.code}',
        otpTransactionId: _id('OTP'),
        otpVerifiedAt: now,
        capturedAt: now,
        capturedByActorId: const ActorId('RM-DEMO'),
        agentId: 'SP-DEMO-778812',
      ),
    );
    _consents[id.value] = granted;
    SafeLog.audit(action: 'CONSENT_GRANTED', actorId: 'RM-DEMO', outcome: 'OK');
    return granted;
  }

  @override
  Future<ConsentGrant?> find(ConsentId id) async => _consents[id.value];

  /// Placeholder statement text.
  ///
  /// **This is not approved regulated copy.** `S05-experience-evidence.md` §5
  /// records that consent statement wording is authored by Compliance and
  /// Legal (OPEN-CNS-02 / S05-OPEN-03). These strings exist so the flow renders
  /// and must be replaced before any pilot.
  String _statementFor(ConsentCode c) =>
      '[PLACEHOLDER — awaiting Compliance-approved wording, S05-OPEN-03] '
      '${c.shortLabel}.';

  // -------------------------------------------------------------------------
  // Suitability
  // -------------------------------------------------------------------------

  @override
  Future<SuitabilityAssessment> start({
    required CustomerId customerId,
    required ConsentGrantRef solicitationConsent,
  }) async {
    if (solicitationConsent.code != ConsentCode.solicitation) {
      throw const RepositoryException(
          'CONSENT_SCOPE_MISMATCH', 'SUIT-R21 requires a CNS-SOL grant');
    }
    final a = SuitabilityAssessment.start(
      id: SuitabilityId(_id('SUIT')),
      customerId: customerId,
      consentProof: ConsentProofPresent(solicitationConsent.consentId),
    );
    _assessments[a.id.value] = a;
    return a;
  }

  @override
  Future<SuitabilityAssessment> complete({
    required SuitabilityId id,
    required SuitabilityAnswers answers,
  }) async {
    final existing = _assessments[id.value];
    if (existing == null) {
      throw const RepositoryException('ASSESSMENT_NOT_FOUND', 'No such assessment');
    }
    final completed = existing.complete(answers, _clock());
    _assessments[id.value] = completed;
    SafeLog.audit(
        action: 'SUITABILITY_COMPLETED', actorId: 'RM-DEMO', outcome: 'EVALUATED');
    return completed;
  }

  // -------------------------------------------------------------------------
  // Catalogue
  // -------------------------------------------------------------------------

  @override
  Future<List<CatalogueProduct>> eligibleProducts({
    required SuitabilityEvaluationRef suitabilityRef,
    required int ageNextBirthday,
  }) async {
    // CAT-R02: suitability filter first, eligibility second.
    return kSampleR0Catalogue
        .where((p) => p.productClass == suitabilityRef.productClass)
        .where((p) => p.eligibleFor(ageNextBirthday))
        .toList();
  }

  // -------------------------------------------------------------------------
  // Quote
  // -------------------------------------------------------------------------

  @override
  Future<Quote> requestQuote({
    required CustomerId customerId,
    required SuitabilityEvaluationRef suitabilityRef,
    required String insurerProductCode,
    required double sumAssured,
    required int termYears,
    required String idempotencyKey,
  }) async {
    final now = _clock();

    // SUIT-R20 conditions re-checked at the point of use (§5.2: "strong at the
    // point of use"). The type guarantees a ref exists; it cannot guarantee the
    // ref is still valid, or that it belongs to THIS customer.
    if (!suitabilityRef.isValidAt(now)) {
      throw const RepositoryException(
          'SUITABILITY_EVALUATION_EXPIRED', 'SUIT-R20: evaluation is older than 30 days');
    }
    if (suitabilityRef.customerId != customerId) {
      SafeLog.warn('SECURITY_EVENT SUITABILITY_SUBJECT_MISMATCH');
      throw const RepositoryException(
          'SUITABILITY_SUBJECT_MISMATCH', 'SUIT-R20: evaluation is bound to another customer');
    }

    // AC-QUOTE-010-2 / INV-IDM-01: idempotent create.
    final existingId = _idempotency[idempotencyKey];
    if (existingId != null) return _quotes[existingId]!;

    final product = kSampleR0Catalogue
        .firstWhere((p) => p.insurerProductCode == insurerProductCode);

    // Deterministic sample pricing. Not a real rate table.
    final base = sumAssured / 100000 * 62.0;
    final offers = <Offer>[
      Offer(
        id: OfferId(_id('OFFER')),
        insurerCode: product.insurerCode,
        insurerDisplayName: product.insurerDisplayName,
        bankProductName: product.bankProductName,
        annualPremium: (base * 1.00).roundToDouble(),
        sumAssured: sumAssured,
        termYears: termYears,
        premiumFrequency: 'ANNUAL',
        // QR-01: shorter of insurer validity and 7 days.
        validUntil: now.add(const Duration(days: 7)),
        keyBenefits: const ['Death benefit', 'Terminal illness cover'],
      ),
      Offer(
        id: OfferId(_id('OFFER')),
        insurerCode: kSampleR0Catalogue[1].insurerCode,
        insurerDisplayName: kSampleR0Catalogue[1].insurerDisplayName,
        bankProductName: kSampleR0Catalogue[1].bankProductName,
        annualPremium: (base * 1.14).roundToDouble(),
        sumAssured: sumAssured,
        termYears: termYears,
        premiumFrequency: 'ANNUAL',
        validUntil: now.add(const Duration(days: 7)),
        keyBenefits: const ['Death benefit', 'Waiver of premium'],
      ),
    ];

    final quote = Quote(
      id: QuoteId(_id('QUOTE')),
      customerId: customerId,
      suitabilityRef: suitabilityRef,
      state: QuoteState.quoted,
      requestedAt: now,
      offers: offers,
    );
    _quotes[quote.id.value] = quote;
    _idempotency[idempotencyKey] = quote.id.value;
    SafeLog.audit(action: 'QUOTE_COMPLETED', actorId: 'RM-DEMO', outcome: 'QUOTED');
    return quote;
  }

  @override
  Future<Quote> pollQuote(QuoteId id) async => _quotes[id.value]!;

  @override
  Future<Quote> selectOffer({
    required QuoteId quoteId,
    required OfferId offerId,
  }) async {
    final q = _quotes[quoteId.value]!;
    final selected = q.select(offerId, _clock());
    _quotes[quoteId.value] = selected;
    SafeLog.audit(action: 'OFFER_SELECTED', actorId: 'RM-DEMO', outcome: 'OK');
    return selected;
  }

  // -------------------------------------------------------------------------
  // Proposal
  // -------------------------------------------------------------------------

  @override
  Future<Proposal> createDraft({
    required QuoteId quoteId,
    required OfferId offerId,
    required CustomerId customerId,
    required ConsentGrantRef sharingConsent,
  }) async {
    final p = Proposal(
      id: ProposalId(_id('PROP')),
      quoteId: quoteId,
      offerId: offerId,
      customerId: customerId,
      sharingConsent: sharingConsent,
      state: ProposalState.draft,
    );
    _proposals[p.id.value] = p;
    return p;
  }

  @override
  Future<Proposal> saveAnswers(ProposalId id, Map<String, String> answers) async {
    final p = _proposals[id.value]!.copyWith(answers: answers);
    _proposals[id.value] = p;
    return p;
  }

  @override
  Future<Proposal> submit({
    required ProposalId id,
    required ConsentGrantRef sharingConsent,
    required String agentId,
    required DateTime now,
  }) async {
    // INV-PRP-01, re-validated at the point of use.
    if (!sharingConsent.isValidAt(now)) {
      throw const RepositoryException(
          'CONSENT_EXPIRED', 'INV-PRP-01: sharing consent has expired');
    }
    if (sharingConsent.code != ConsentCode.sharing) {
      throw const RepositoryException(
          'CONSENT_SCOPE_MISMATCH', 'CNS-R22: consent does not cover insurer sharing');
    }
    // AC-PROP-030-1: no agentId, no insurer call.
    if (agentId.isEmpty) {
      throw const RepositoryException(
          'AGENT_ATTRIBUTION_MISSING', 'BR-PROP-030: agentId is mandatory');
    }
    final p = _proposals[id.value]!.copyWith(
      state: ProposalState.underWriting,
      insurerApplicationRef: _id('APP'),
      submittedAt: now,
    );
    _proposals[id.value] = p;
    SafeLog.audit(action: 'PROPOSAL_SUBMITTED', actorId: 'RM-DEMO', outcome: 'OK');
    return p;
  }

  @override
  Future<Proposal> refreshStatus(ProposalId id) async {
    final p = _proposals[id.value]!;
    final next = switch (p.state) {
      ProposalState.underWriting => p.copyWith(state: ProposalState.uwApproved),
      ProposalState.uwApproved => p.copyWith(state: ProposalState.awaitingPayment),
      _ => p,
    };
    _proposals[id.value] = next;
    return next;
  }

  // -------------------------------------------------------------------------
  // Payment — device isolated
  // -------------------------------------------------------------------------

  @override
  Future<PaymentHandoff> issueCustomerDeviceLink({required ProposalId proposalId}) async {
    final proposal = _proposals[proposalId.value]!;
    final quote = _quotes[proposal.quoteId.value]!;
    final offer = quote.selectedOffer!;
    final now = _clock();
    final payment = Payment(
      id: PaymentId(_id('PAY')),
      proposalId: proposalId,
      state: PaymentState.linkIssued,
      amount: offer.annualPremium,
    );
    _payments[payment.id.value] = payment;

    final customer = _customers.firstWhere((c) => c.id == proposal.customerId);

    SafeLog.audit(
        action: 'PAYMENT_LINK_ISSUED', actorId: 'RM-DEMO', outcome: 'CUSTOMER_DEVICE');

    return PaymentHandoff(
      paymentId: payment.id,
      customerDeviceUrl:
          'https://pg.example-aubank.invalid/pay/${payment.id.value}',
      sentTo: customer.maskedMobile,
      expiresAt: now.add(const Duration(minutes: 30)),
      amount: offer.annualPremium,
    );
  }

  @override
  Future<Payment> pollStatus(PaymentId id) async => _payments[id.value]!;

  @override
  Future<Payment> simulateCustomerCompletesOnOwnDevice(PaymentId id) async {
    // Straight to RECONCILED, because INV-POL-01 requires reconciliation — not
    // capture — before issuance. A fake that stopped at CAPTURED would let the
    // UI imply issuance is permitted when it is not.
    final p = Payment(
      id: id,
      proposalId: _payments[id.value]!.proposalId,
      state: PaymentState.reconciled,
      amount: _payments[id.value]!.amount,
      pgTransactionRef: _id('PGTXN'),
    );
    _payments[id.value] = p;
    final proposal = _proposals[p.proposalId.value]!;
    _proposals[p.proposalId.value] = proposal.copyWith(state: ProposalState.paid);
    SafeLog.audit(action: 'PAYMENT_RECONCILED', actorId: 'SYSTEM', outcome: 'OK');
    return p;
  }

  // -------------------------------------------------------------------------
  // Policy
  // -------------------------------------------------------------------------

  @override
  Future<Policy> requestIssuance({
    required ProposalId proposalId,
    required Payment payment,
  }) async {
    // INV-POL-01.
    if (!payment.permitsIssuance) {
      throw const RepositoryException(
          'PAYMENT_NOT_RECONCILED', 'INV-POL-01: issuance requires a reconciled payment');
    }
    final policy = Policy(
      id: PolicyId(_id('POL')),
      proposalId: proposalId,
      state: PolicyState.active,
      policyNumber: 'SMPL/${_id('PN')}',
      issuanceConfirmed: true,
      paymentReconciled: true,
      auditPersisted: true,
    );
    _policies[policy.id.value] = policy;
    SafeLog.audit(action: 'POLICY_ISSUED', actorId: 'SYSTEM', outcome: 'ACTIVE');
    return policy;
  }

  @override
  Future<Policy> pollPolicy(PolicyId id) async => _policies[id.value]!;
}

/// Convenience: the demo RM identity used throughout the fake.
const kDemoActor = ActorId('RM-DEMO');
const kDemoAgentId = 'SP-DEMO-778812';

/// The stage the demo journey starts at.
const kInitialStage = JourneyStage.initiated;
