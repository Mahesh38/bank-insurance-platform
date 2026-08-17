/// The journey saga — client-side projection.
///
/// Stages and legal transitions are transcribed from the orchestrated saga in
/// `01-domain-model-and-invariants.md` §5. The RM app is a **conformist**
/// downstream of Journey Orchestration (relationship R-01): it renders the
/// published stage model and does not invent one. When the real BFF is wired
/// in, this enum is the contract it conforms to.
///
/// INV-JRN-02 is honoured here: a [JourneyStage] carries no business decision —
/// no eligibility, no price, no underwriting outcome. Those live on their own
/// aggregates and are referenced.
library;

enum JourneyStage {
  initiated,
  needAnalysis,
  suitabilityComplete,
  consentCaptured,
  quoting,
  quoteSelected,
  proposalInProgress,
  underwriting,
  paymentPending,
  paymentSettled,
  issuancePending,
  issued,
  sold,
  abandoned,
  declined,
  compensating,
  manualIntervention,
  compensated;

  bool get isTerminal => switch (this) {
        JourneyStage.sold ||
        JourneyStage.abandoned ||
        JourneyStage.declined ||
        JourneyStage.compensated =>
          true,
        _ => false,
      };
}

/// Legal transitions, exactly as drawn in §5. A transition not listed here is
/// illegal and is rejected by [JourneyState.advanceTo] — INV-JRN-01.
const Map<JourneyStage, Set<JourneyStage>> kLegalTransitions = {
  JourneyStage.initiated: {JourneyStage.needAnalysis},
  JourneyStage.needAnalysis: {
    JourneyStage.suitabilityComplete,
    JourneyStage.abandoned,
  },
  JourneyStage.suitabilityComplete: {
    JourneyStage.consentCaptured,
    JourneyStage.abandoned,
  },
  JourneyStage.consentCaptured: {JourneyStage.quoting, JourneyStage.abandoned},
  JourneyStage.quoting: {JourneyStage.quoteSelected, JourneyStage.abandoned},
  JourneyStage.quoteSelected: {
    JourneyStage.proposalInProgress,
    JourneyStage.abandoned,
  },
  JourneyStage.proposalInProgress: {
    JourneyStage.underwriting,
    JourneyStage.abandoned,
  },
  JourneyStage.underwriting: {
    JourneyStage.paymentPending,
    JourneyStage.declined,
  },
  JourneyStage.paymentPending: {
    JourneyStage.paymentSettled,
    JourneyStage.abandoned,
  },
  JourneyStage.paymentSettled: {
    JourneyStage.issuancePending,
    JourneyStage.compensating,
  },
  JourneyStage.issuancePending: {JourneyStage.issued, JourneyStage.compensating},
  JourneyStage.issued: {JourneyStage.sold, JourneyStage.compensating},
  // INV-JRN-04: a compensating journey is never silently closed. It exits only
  // to SOLD, COMPENSATED or MANUAL_INTERVENTION.
  JourneyStage.compensating: {
    JourneyStage.sold,
    JourneyStage.compensated,
    JourneyStage.manualIntervention,
  },
  JourneyStage.manualIntervention: {
    JourneyStage.sold,
    JourneyStage.compensated,
  },
  JourneyStage.sold: {},
  JourneyStage.abandoned: {},
  JourneyStage.declined: {},
  JourneyStage.compensated: {},
};

bool isLegalTransition(JourneyStage from, JourneyStage to) =>
    kLegalTransitions[from]?.contains(to) ?? false;

/// Human-readable stage labels for the `ProgressStepper` component.
String stageLabel(JourneyStage s) => switch (s) {
      JourneyStage.initiated => 'Started',
      JourneyStage.needAnalysis => 'Need analysis',
      JourneyStage.suitabilityComplete => 'Suitability complete',
      JourneyStage.consentCaptured => 'Consent captured',
      JourneyStage.quoting => 'Quoting',
      JourneyStage.quoteSelected => 'Offer selected',
      JourneyStage.proposalInProgress => 'Proposal',
      JourneyStage.underwriting => 'Underwriting',
      JourneyStage.paymentPending => 'Awaiting payment',
      JourneyStage.paymentSettled => 'Payment settled',
      JourneyStage.issuancePending => 'Awaiting issuance',
      JourneyStage.issued => 'Issued',
      JourneyStage.sold => 'Sold',
      JourneyStage.abandoned => 'Abandoned',
      JourneyStage.declined => 'Declined',
      JourneyStage.compensating => 'Recovering',
      JourneyStage.manualIntervention => 'Manual intervention',
      JourneyStage.compensated => 'Refunded',
    };

/// The ordered happy path, for the stepper. Exception stages are not steps.
const List<JourneyStage> kHappyPath = [
  JourneyStage.initiated,
  JourneyStage.needAnalysis,
  JourneyStage.suitabilityComplete,
  JourneyStage.consentCaptured,
  JourneyStage.quoting,
  JourneyStage.quoteSelected,
  JourneyStage.proposalInProgress,
  JourneyStage.underwriting,
  JourneyStage.paymentPending,
  JourneyStage.paymentSettled,
  JourneyStage.issuancePending,
  JourneyStage.issued,
  JourneyStage.sold,
];
