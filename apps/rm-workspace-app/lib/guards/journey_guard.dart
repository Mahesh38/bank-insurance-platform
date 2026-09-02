/// Route guards — the second structural layer.
///
/// The first layer is the type system: `SuitabilityEvaluationRef` and
/// `ConsentGrantRef` cannot be constructed without the thing they prove, so the
/// repository calls cannot be made. That stops the *action*.
///
/// This layer stops the *navigation*. A guard is consulted by
/// `onGenerateRoute` before a screen is constructed, so an ungated route does
/// not render a screen with a disabled button — it does not render the screen
/// at all. Deep-linking straight to `/quote` lands on [GuardBlocked], not on a
/// quote form.
///
/// Two layers because the failure this app exists to prevent — a quote reaching
/// an insurer with no suitability evaluation behind it — is the one defect the
/// programme has already shipped once.
library;

import '../domain/consent.dart';
import '../domain/journey.dart';
import '../state/journey_controller.dart';

enum AppRoute {
  login('/login'),
  pipeline('/pipeline'),
  customerSearch('/customer-search'),
  customerDetail('/customer-detail'),
  leadCreate('/lead-create'),
  consent('/consent'),
  prefillReview('/prefill-review'),
  suitability('/suitability'),
  suitabilityOutcome('/suitability-outcome'),
  products('/products'),
  quote('/quote'),
  compare('/compare'),
  proposal('/proposal'),
  submit('/submit'),
  status('/status'),
  payment('/payment'),
  confirmation('/confirmation'),
  funnel('/funnel');

  final String path;
  const AppRoute(this.path);

  static AppRoute? fromPath(String path) {
    for (final r in AppRoute.values) {
      if (r.path == path) return r;
    }
    return null;
  }
}

/// Why a route was refused. Codes match the acceptance criteria in
/// `S03-requirements-evidence.md` §4 so a blocked screen can cite the rule that
/// blocked it.
class GuardRefusal {
  final String code;
  final String rule;
  final String rmMessage;
  final AppRoute recoverAt;

  const GuardRefusal({
    required this.code,
    required this.rule,
    required this.rmMessage,
    required this.recoverAt,
  });
}

abstract final class JourneyGuard {
  /// Returns `null` when the route may be entered, or a [GuardRefusal].
  ///
  /// Ordered so the most specific precondition wins — an RM with no customer
  /// selected should be told that, not told about suitability.
  static GuardRefusal? check(AppRoute route, JourneyController c) {
    // Every route except login requires an authenticated session — AC-SEC-010-1.
    if (route != AppRoute.login && !c.signedIn) {
      return const GuardRefusal(
        code: 'UNAUTHENTICATED',
        rule: 'AC-SEC-010-1',
        rmMessage: 'Sign in to continue.',
        recoverAt: AppRoute.login,
      );
    }

    switch (route) {
      case AppRoute.login:
      case AppRoute.pipeline:
      case AppRoute.customerSearch:
      case AppRoute.funnel:
        return null;

      case AppRoute.customerDetail:
      case AppRoute.leadCreate:
        return c.customer == null ? _noCustomer : null;

      case AppRoute.consent:
      case AppRoute.prefillReview:
        if (c.customer == null) return _noCustomer;
        if (c.lead == null) return _noLead;
        return null;

      case AppRoute.suitability:
      case AppRoute.suitabilityOutcome:
        if (c.customer == null) return _noCustomer;
        if (c.lead == null) return _noLead;
        // SUIT-R21: no need-analysis without CNS-SOL consent.
        if (c.consentRef(ConsentCode.solicitation) == null) {
          return _noSolicitationConsent;
        }
        return null;

      // ---------------------------------------------------------------
      // THE SUITABILITY HARD-GATE — SUIT-R20 / INV-QUO-01, control C1
      // ---------------------------------------------------------------
      case AppRoute.products:
      case AppRoute.quote:
      case AppRoute.compare:
        if (c.customer == null) return _noCustomer;
        if (c.lead == null) return _noLead;
        if (!c.hasQuotePermission) return _noSuitability;
        return null;

      // ---------------------------------------------------------------
      // THE CONSENT GATE — INV-PRP-01, control C2
      // ---------------------------------------------------------------
      case AppRoute.proposal:
      case AppRoute.submit:
        if (!c.hasQuotePermission) return _noSuitability;
        if (c.quote?.selectedOfferId == null) return _noSelectedOffer;
        if (!c.hasSharingConsent) return _noSharingConsent;
        return null;

      case AppRoute.status:
        return c.proposal == null ? _noProposal : null;

      case AppRoute.payment:
        if (c.proposal == null) return _noProposal;
        // INV-PRP-03: payment cannot be reached before underwriting approval.
        if (c.stage != JourneyStage.paymentPending &&
            c.stage != JourneyStage.paymentSettled) {
          return _notAwaitingPayment;
        }
        return null;

      case AppRoute.confirmation:
        return c.policy == null ? _noPolicy : null;
    }
  }

  static const _noCustomer = GuardRefusal(
    code: 'CUSTOMER_NOT_SELECTED',
    rule: 'BR-CUST-010',
    rmMessage: 'Select a customer before continuing.',
    recoverAt: AppRoute.customerSearch,
  );

  static const _noLead = GuardRefusal(
    code: 'LEAD_NOT_CREATED',
    rule: 'BR-LEAD-010',
    rmMessage: 'Create a lead for this customer before continuing.',
    recoverAt: AppRoute.leadCreate,
  );

  static const _noSolicitationConsent = GuardRefusal(
    code: 'CONSENT_REQUIRED',
    rule: 'SUIT-R21 / CNS-SOL',
    rmMessage:
        'The customer must consent to the need analysis before it can start.',
    recoverAt: AppRoute.consent,
  );

  static const _noSuitability = GuardRefusal(
    code: 'SUITABILITY_EVALUATION_REQUIRED',
    rule: 'SUIT-R20 / INV-QUO-01 (control C1, non-waivable)',
    rmMessage:
        'A completed suitability evaluation is required before any quote can be '
        'requested. This cannot be skipped or overridden.',
    recoverAt: AppRoute.suitability,
  );

  static const _noSelectedOffer = GuardRefusal(
    code: 'OFFER_NOT_SELECTED',
    rule: 'AC-COMP-010-2',
    rmMessage: 'Select an offer before starting the proposal.',
    recoverAt: AppRoute.compare,
  );

  static const _noSharingConsent = GuardRefusal(
    code: 'CONSENT_REQUIRED',
    rule: 'INV-PRP-01 / CNS-SHR (control C2, non-waivable)',
    rmMessage:
        'The customer must consent to their details being shared with the '
        'selected insurer before the proposal can proceed.',
    recoverAt: AppRoute.proposal,
  );

  static const _noProposal = GuardRefusal(
    code: 'PROPOSAL_NOT_CREATED',
    rule: 'BR-PROP-010',
    rmMessage: 'Start the proposal before continuing.',
    recoverAt: AppRoute.compare,
  );

  static const _notAwaitingPayment = GuardRefusal(
    code: 'NOT_AWAITING_PAYMENT',
    rule: 'INV-PRP-03',
    rmMessage: 'Payment opens once underwriting has approved the application.',
    recoverAt: AppRoute.status,
  );

  static const _noPolicy = GuardRefusal(
    code: 'POLICY_NOT_ISSUED',
    rule: 'BR-POL-010',
    rmMessage: 'No policy has been issued for this journey yet.',
    recoverAt: AppRoute.status,
  );
}
