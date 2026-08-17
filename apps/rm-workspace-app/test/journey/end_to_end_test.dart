/// The complete R0 assisted Term journey, driven through the in-memory backend.
///
/// **This is not S11 evidence.** It proves the RM application's client-side
/// journey holds together against a fake. The bounded contexts behind these
/// interfaces do not exist, so nothing here says the platform works — only that
/// the interface half is coherent and the gates hold at every step.
library;

import 'package:flutter_test/flutter_test.dart';
import 'package:rm_workspace_app/domain/consent.dart';
import 'package:rm_workspace_app/domain/journey.dart';
import 'package:rm_workspace_app/domain/sales.dart';
import 'package:rm_workspace_app/domain/suitability.dart';
import 'package:rm_workspace_app/logging/safe_log.dart';
import 'package:rm_workspace_app/state/journey_controller.dart';

import '../support/journey_fixtures.dart';

void main() {
  setUp(SafeLog.clear);

  test('lead → suitability → consent → quote → proposal → customer-device '
      'payment → issuance → reconciliation → audit', () async {
    final c = JourneyController.withFake();

    // --- sign in -----------------------------------------------------------
    c.signIn();
    expect(c.stage, JourneyStage.initiated);

    // --- customer and lead -------------------------------------------------
    final results = await c.searchCustomers('AU100');
    expect(results, isNotEmpty);
    await c.selectCustomer(results.first);
    await c.createLead();

    expect(c.lead, isNotNull);
    expect(c.stage, JourneyStage.needAnalysis);

    // --- consent -----------------------------------------------------------
    // Before consent, no suitability may start.
    expect(c.consentRef(ConsentCode.solicitation), isNull);

    await c.requestConsents();
    expect(c.consentGrants[ConsentCode.solicitation]!.state,
        ConsentState.otpPending);

    await c.customerVerifiedConsents();
    expect(c.consentRef(ConsentCode.solicitation), isNotNull);
    expect(c.consentRef(ConsentCode.dataProcessing), isNotNull);

    // Evidence is complete — CNS-R01.
    final evidence = c.consentGrants[ConsentCode.solicitation]!.evidence!;
    expect(evidence.otpTransactionId, isNotEmpty);
    expect(evidence.statementVersion, isNotEmpty);
    expect(evidence.statementTextHash, isNotEmpty);
    expect(evidence.agentId, isNotEmpty);

    // --- suitability -------------------------------------------------------
    await c.startSuitability();
    expect(c.assessment!.state, AssessmentState.inProgress);
    expect(c.hasQuotePermission, isFalse, reason: 'not evaluated yet');

    await c.completeSuitability(suitableAnswers);
    expect(c.assessment!.state, AssessmentState.completed);
    expect(c.stage, JourneyStage.suitabilityComplete);
    expect(c.hasQuotePermission, isTrue);

    // --- products and quote ------------------------------------------------
    await c.loadEligibleProducts();
    expect(c.eligibleProducts, isNotEmpty);
    expect(c.stage, JourneyStage.consentCaptured);

    await c.requestQuote(
      product: c.eligibleProducts.first,
      sumAssured: 5000000,
      termYears: 25,
    );
    expect(c.lastError, isNull);
    expect(c.quote!.state, QuoteState.quoted);
    expect(c.quote!.offers.length, 2);
    expect(c.stage, JourneyStage.quoting);

    // The quote carries the evaluation it was gated on.
    expect(c.quote!.suitabilityRef.suitabilityId, c.assessment!.id);

    // QR-06: ranked ascending by premium.
    final ranked = c.quote!.rankedOffers;
    expect(ranked.first.annualPremium,
        lessThanOrEqualTo(ranked.last.annualPremium));

    // --- select ------------------------------------------------------------
    await c.selectOffer(ranked.first.id);
    expect(c.quote!.selectedOfferId, ranked.first.id);
    expect(c.stage, JourneyStage.quoteSelected);

    // --- sharing consent and proposal --------------------------------------
    expect(c.hasSharingConsent, isFalse);
    await c.captureSharingConsent();
    expect(c.hasSharingConsent, isTrue);

    await c.createProposal();
    expect(c.proposal!.state, ProposalState.draft);
    expect(c.stage, JourneyStage.proposalInProgress);

    await c.saveProposalAnswers({'nomineeName': 'A Nominee'});
    await c.submitProposal();
    expect(c.lastError, isNull);
    expect(c.proposal!.state, ProposalState.underWriting);
    expect(c.proposal!.insurerApplicationRef, isNotNull);
    expect(c.stage, JourneyStage.underwriting);

    // --- underwriting ------------------------------------------------------
    await c.refreshUnderwriting();
    expect(c.proposal!.state, ProposalState.uwApproved);
    await c.refreshUnderwriting();
    expect(c.proposal!.state, ProposalState.awaitingPayment);
    expect(c.stage, JourneyStage.paymentPending);

    // --- payment, on the customer's device ---------------------------------
    await c.issuePaymentLink();
    expect(c.paymentHandoff, isNotNull);
    expect(c.paymentHandoff!.customerDeviceUrl, startsWith('https://'));
    expect(c.payment!.state, PaymentState.linkIssued);
    expect(c.payment!.permitsIssuance, isFalse);

    await c.customerCompletesPayment();
    expect(c.payment!.state, PaymentState.reconciled);
    expect(c.payment!.permitsIssuance, isTrue);
    expect(c.stage, JourneyStage.paymentSettled);

    // --- issuance and reconciliation ---------------------------------------
    await c.issuePolicy();
    expect(c.lastError, isNull);
    expect(c.policy, isNotNull);
    expect(c.policy!.policyNumber, isNotNull);

    // --- sold, all four conditions -----------------------------------------
    expect(c.policy!.issuanceConfirmed, isTrue);
    expect(c.policy!.paymentReconciled, isTrue);
    expect(c.policy!.auditPersisted, isTrue);
    expect(c.policy!.isSold, isTrue);
    expect(c.stage, JourneyStage.sold);

    // --- audit -------------------------------------------------------------
    final actions = SafeLog.records
        .where((r) => r.message.startsWith('AUDIT'))
        .map((r) => r.message)
        .toList();

    for (final expected in const [
      'LEAD_CREATED',
      'CONSENT_OTP_DISPATCHED',
      'CONSENT_GRANTED',
      'SUITABILITY_COMPLETED',
      'QUOTE_COMPLETED',
      'OFFER_SELECTED',
      'PROPOSAL_SUBMITTED',
      'PAYMENT_LINK_ISSUED',
      'PAYMENT_RECONCILED',
      'POLICY_ISSUED',
    ]) {
      expect(actions.any((a) => a.contains(expected)), isTrue,
          reason: 'INV-AUD-02: no audit event emitted for $expected');
    }
  });

  test('the journey refuses to skip suitability, at every layer', () async {
    final c = await withConsent(); // consented, not evaluated

    // The controller will not request a quote without a token.
    await c.requestQuote(
      product: kSampleProduct,
      sumAssured: 5000000,
      termYears: 25,
    );

    expect(c.quote, isNull);
    expect(c.lastError, contains('SUITABILITY_EVALUATION_REQUIRED'));
    expect(c.stage, JourneyStage.needAnalysis,
        reason: 'the journey must not have advanced');
  });

  test('INV-POL-01 — issuance is refused, and the saga does not advance, when '
      'the payment is not reconciled', () async {
    final c = await withSharingConsent();
    await c.createProposal();
    await c.submitProposal();
    await c.refreshUnderwriting();
    await c.refreshUnderwriting();
    await c.issuePaymentLink();

    // The customer has not paid.
    expect(c.payment!.state, PaymentState.linkIssued);
    expect(c.stage, JourneyStage.paymentPending);

    await c.issuePolicy();

    expect(c.policy, isNull);
    expect(c.lastError, contains('PAYMENT_NOT_RECONCILED'));
    expect(c.stage, JourneyStage.paymentPending,
        reason: 'the saga must not advance past a precondition it failed');
  });
}

/// A catalogue row for the negative test, so it does not depend on a loaded
/// product list — the point of that test is that the quote is refused before
/// the catalogue is ever consulted.
const kSampleProduct = CatalogueProduct(
  insurerCode: 'SAMPLE-A1',
  insurerDisplayName: 'Sample Insurer A',
  bankProductName: 'Term Protect (sample)',
  insurerProductCode: 'SMPL-TERM-01',
  productClass: ProductClass.term,
  insurerGroup: 'A',
  minAge: 18,
  maxAge: 65,
  minSumAssured: 500000,
  maxSumAssured: 100000000,
);
