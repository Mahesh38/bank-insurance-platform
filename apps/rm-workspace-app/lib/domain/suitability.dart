/// Suitability and need analysis — algorithm `SUIT-ALGO-LIFE-v1.0`.
///
/// Implements `docs/au-bank-insurance-platform/rule-packs/suitability-rule-pack.md`
/// §2 (question set), §3 (deterministic computation) and §4 (product-class
/// outcomes), and the state machine in `01-domain-model-and-invariants.md` §4.3.
///
/// ## The structural guard
///
/// [SuitabilityEvaluationRef] has a library-private constructor. The ONLY way
/// to obtain one is [SuitabilityAssessment.evaluationRef], which returns `null`
/// unless the assessment is `COMPLETED`, unexpired, and carries an outcome of
/// `SUITABLE` or `SUITABLE_WITH_CAUTION` for the requested product class.
///
/// The quote path takes a `SuitabilityEvaluationRef` parameter. "Request a
/// quote without a suitability evaluation" therefore does not compile — which
/// is INV-QUO-01 / SUIT-R20 (control C1, non-waivable) enforced at the type
/// level rather than by a disabled button.
library;

import 'ids.dart';

/// R0 product classes. Term only is in R0 scope; Savings and ULIP are modelled
/// because the algorithm covers them (rule pack §4.3, §4.4) and omitting them
/// would hard-code a single-class assumption.
enum ProductClass { term, savingsEndowment, ulip }

/// Suitability outcome vocabulary — rule pack §4.1.
enum SuitabilityOutcome {
  suitable,
  suitableWithCaution,
  notSuitable,
  insufficientData;

  /// SUIT-R20 condition 4: the outcome must permit the requested class.
  bool get permitsQuote =>
      this == SuitabilityOutcome.suitable ||
      this == SuitabilityOutcome.suitableWithCaution;

  /// Most-restrictive-wins precedence, rule pack §4.4.
  int get severity => switch (this) {
        SuitabilityOutcome.suitable => 0,
        SuitabilityOutcome.suitableWithCaution => 1,
        SuitabilityOutcome.insufficientData => 2,
        SuitabilityOutcome.notSuitable => 3,
      };
}

enum AssessmentState { inProgress, completed, superseded, expired, abandoned }

enum FinancialObjective { protection, savingsGoal, retirement, wealthGrowth, taxPlanning }

enum InvestmentHorizon { lt5y, y5to10, y10to20, gt20y }

enum RiskAttitude { capitalProtection, low, moderate, high }

enum OccupationCategory { salaried, selfEmployedProf, selfEmployedBusiness, agriculture, other }

enum PriorDeclineAnswer { no, yes, dontKnow }

/// The twelve inputs — rule pack §2. Four are derived from CBS, eight asked.
class SuitabilityAnswers {
  // Derived (SQ-D01..D04)
  final int ageNextBirthday;
  final int relationshipTenureMonths;
  final double knownExistingCover;
  final double knownLiabilities;

  // Asked (SQ-01..SQ-12)
  final double annualIncome;
  final int dependants;
  final double declaredExistingCover;
  final double declaredLiabilities;
  final FinancialObjective objective;
  final InvestmentHorizon horizon;
  final RiskAttitude riskAttitude;
  final double declaredAnnualPremium;
  final bool tobaccoUse;
  final OccupationCategory occupation;
  final PriorDeclineAnswer priorDecline;
  final bool isReplacement;

  const SuitabilityAnswers({
    required this.ageNextBirthday,
    this.relationshipTenureMonths = 0,
    this.knownExistingCover = 0,
    this.knownLiabilities = 0,
    required this.annualIncome,
    required this.dependants,
    required this.declaredExistingCover,
    required this.declaredLiabilities,
    required this.objective,
    required this.horizon,
    required this.riskAttitude,
    required this.declaredAnnualPremium,
    required this.tobaccoUse,
    required this.occupation,
    required this.priorDecline,
    required this.isReplacement,
  });

  /// SUIT-R03: where a self-declared value is lower than the bank-known value,
  /// the higher value is used and the discrepancy recorded.
  double get effectiveExistingCover =>
      declaredExistingCover > knownExistingCover ? declaredExistingCover : knownExistingCover;

  double get effectiveLiabilities =>
      declaredLiabilities > knownLiabilities ? declaredLiabilities : knownLiabilities;

  List<String> get dataInconsistencyFlags => [
        if (declaredExistingCover < knownExistingCover) 'EXISTING_COVER_UNDERSTATED',
        if (declaredLiabilities < knownLiabilities) 'LIABILITIES_UNDERSTATED',
      ];

  /// SUIT-R04: a prior decline sets the underwriting risk flag but does not by
  /// itself change the outcome.
  bool get underwritingRiskFlag => priorDecline != PriorDeclineAnswer.no;
}

/// Derived values — rule pack §3.3.
class SuitabilityDerived {
  final int incomeMultiple;
  final double ageFactor;
  final double recommendedCover;
  final double protectionGap;
  final double affordabilityRatio;

  const SuitabilityDerived({
    required this.incomeMultiple,
    required this.ageFactor,
    required this.recommendedCover,
    required this.protectionGap,
    required this.affordabilityRatio,
  });
}

/// A single class outcome with every fired reason code — SUIT-R19a.
class ClassOutcome {
  final ProductClass productClass;
  final SuitabilityOutcome outcome;
  final List<String> reasonCodes;

  const ClassOutcome(this.productClass, this.outcome, this.reasonCodes);
}

/// Proof that a valid suitability evaluation exists for a product class.
///
/// Cannot be constructed outside this library.
class SuitabilityEvaluationRef {
  final SuitabilityId suitabilityId;
  final CustomerId customerId;
  final ProductClass productClass;
  final SuitabilityOutcome outcome;
  final DateTime validUntil;

  const SuitabilityEvaluationRef._({
    required this.suitabilityId,
    required this.customerId,
    required this.productClass,
    required this.outcome,
    required this.validUntil,
  });

  bool isValidAt(DateTime now) => now.isBefore(validUntil);

  @override
  String toString() =>
      'SuitabilityEvaluationRef(${suitabilityId.value}, ${productClass.name}, ${outcome.name})';
}

/// `SUIT-ALGO-LIFE-v1.0` — pure, deterministic, reproducible (SUIT-R05).
abstract final class SuitabilityAlgorithm {
  static const version = 'SUIT-ALGO-LIFE-v1.0';
  static const questionnaireVersion = 'SUIT-QSET-LIFE-v1.0';

  /// SUIT-R20 condition 2: the validity window.
  static const validity = Duration(days: 30);

  /// Income bands — rule pack §3.1.
  static int incomeMultipleFor(double annualIncome) {
    if (annualIncome < 300000) return 10;
    if (annualIncome < 500000) return 12;
    if (annualIncome < 1000000) return 15;
    if (annualIncome < 2000000) return 18;
    if (annualIncome < 3500000) return 20;
    if (annualIncome < 5000000) return 20;
    if (annualIncome < 10000000) return 18;
    return 15;
  }

  /// Age adjustment — rule pack §3.2. Returns `null` when the age is outside
  /// the eligible range, which SUIT-R08 turns into `NOT_SUITABLE`.
  static double? ageFactorFor(int ageNextBirthday) {
    if (ageNextBirthday < 18 || ageNextBirthday > 65) return null;
    if (ageNextBirthday <= 30) return 1.10;
    if (ageNextBirthday <= 40) return 1.00;
    if (ageNextBirthday <= 50) return 0.85;
    if (ageNextBirthday <= 60) return 0.65;
    return 0.45;
  }

  /// SUIT-R06: rounds up to the next multiple of 1,00,000.
  static double roundUpToLakh(double v) {
    const lakh = 100000.0;
    return (v / lakh).ceil() * lakh;
  }

  static SuitabilityDerived derive(SuitabilityAnswers a) {
    final multiple = incomeMultipleFor(a.annualIncome);
    final factor = ageFactorFor(a.ageNextBirthday) ?? 0.0;
    final recommended = roundUpToLakh(
      (a.annualIncome * multiple * factor) +
          a.effectiveLiabilities +
          (a.dependants * 500000),
    );
    final gap = (recommended - a.effectiveExistingCover)
        .clamp(0.0, double.infinity)
        .toDouble();
    final affordability =
        a.annualIncome > 0 ? a.declaredAnnualPremium / a.annualIncome : double.infinity;
    return SuitabilityDerived(
      incomeMultiple: multiple,
      ageFactor: factor,
      recommendedCover: recommended,
      protectionGap: gap,
      affordabilityRatio: affordability,
    );
  }

  /// Evaluates every product class. Most restrictive outcome wins; every fired
  /// reason code is retained (SUIT-R19a).
  static List<ClassOutcome> evaluate(SuitabilityAnswers a, SuitabilityDerived d) {
    // SUIT-R08 — age outside the eligible range blocks every class.
    if (ageFactorFor(a.ageNextBirthday) == null) {
      return ProductClass.values
          .map((c) => ClassOutcome(
              c, SuitabilityOutcome.notSuitable, const ['AGE_OUTSIDE_ELIGIBLE_RANGE']))
          .toList();
    }
    return [
      _term(a, d),
      _savings(a, d),
      _ulip(a, d),
    ];
  }

  static ClassOutcome _term(SuitabilityAnswers a, SuitabilityDerived d) {
    final fired = <(SuitabilityOutcome, String)>[];
    // SUIT-R09
    if (d.protectionGap > 0) {
      fired.add((SuitabilityOutcome.suitable, 'PROTECTION_GAP_PRESENT'));
    }
    // SUIT-R10
    if (d.protectionGap == 0) {
      fired.add((SuitabilityOutcome.suitableWithCaution, 'COVER_ALREADY_ADEQUATE'));
    }
    // SUIT-R11
    if (d.affordabilityRatio > 0.15) {
      fired.add(
          (SuitabilityOutcome.suitableWithCaution, 'PREMIUM_HIGH_RELATIVE_TO_INCOME'));
    }
    // SUIT-R12
    if (d.affordabilityRatio > 0.30) {
      fired.add((SuitabilityOutcome.notSuitable, 'PREMIUM_UNAFFORDABLE'));
    }
    return _resolve(ProductClass.term, fired);
  }

  static ClassOutcome _savings(SuitabilityAnswers a, SuitabilityDerived d) {
    final fired = <(SuitabilityOutcome, String)>[];
    // SUIT-R13
    final objectiveOk = a.objective == FinancialObjective.savingsGoal ||
        a.objective == FinancialObjective.retirement ||
        a.objective == FinancialObjective.taxPlanning;
    final horizonOk = a.horizon != InvestmentHorizon.lt5y;
    if (objectiveOk && horizonOk && d.affordabilityRatio <= 0.20) {
      fired.add((SuitabilityOutcome.suitable, 'OBJECTIVE_AND_HORIZON_MATCH'));
    }
    // SUIT-R14
    if (a.horizon == InvestmentHorizon.lt5y) {
      fired.add((
        SuitabilityOutcome.notSuitable,
        'HORIZON_TOO_SHORT_FOR_PRODUCT_CLASS'
      ));
    }
    // SUIT-R15
    if (d.protectionGap > 0 && a.objective == FinancialObjective.protection) {
      fired.add((
        SuitabilityOutcome.suitableWithCaution,
        'PROTECTION_NEED_UNMET_BY_THIS_CLASS'
      ));
    }
    return _resolve(ProductClass.savingsEndowment, fired);
  }

  static ClassOutcome _ulip(SuitabilityAnswers a, SuitabilityDerived d) {
    final fired = <(SuitabilityOutcome, String)>[];
    final horizonOk = a.horizon == InvestmentHorizon.y10to20 ||
        a.horizon == InvestmentHorizon.gt20y;
    final objectiveOk = a.objective == FinancialObjective.wealthGrowth ||
        a.objective == FinancialObjective.retirement;
    final riskOk = a.riskAttitude == RiskAttitude.moderate ||
        a.riskAttitude == RiskAttitude.high;
    // SUIT-R16
    if (riskOk && horizonOk && objectiveOk && d.affordabilityRatio <= 0.20) {
      fired.add((SuitabilityOutcome.suitable, 'RISK_HORIZON_OBJECTIVE_MATCH'));
    }
    // SUIT-R17
    if (a.riskAttitude == RiskAttitude.capitalProtection) {
      fired.add((SuitabilityOutcome.notSuitable, 'RISK_ATTITUDE_INCOMPATIBLE'));
    }
    // SUIT-R18
    if (a.horizon == InvestmentHorizon.lt5y || a.horizon == InvestmentHorizon.y5to10) {
      fired.add((
        SuitabilityOutcome.notSuitable,
        'HORIZON_TOO_SHORT_FOR_PRODUCT_CLASS'
      ));
    }
    // SUIT-R19
    if (a.riskAttitude == RiskAttitude.low &&
        horizonOk &&
        objectiveOk &&
        d.affordabilityRatio <= 0.20) {
      fired.add((
        SuitabilityOutcome.suitableWithCaution,
        'RISK_ATTITUDE_BELOW_PRODUCT_PROFILE'
      ));
    }
    return _resolve(ProductClass.ulip, fired);
  }

  static ClassOutcome _resolve(
      ProductClass c, List<(SuitabilityOutcome, String)> fired) {
    if (fired.isEmpty) {
      return ClassOutcome(c, SuitabilityOutcome.notSuitable, const ['NO_RULE_MATCHED']);
    }
    var worst = fired.first.$1;
    for (final f in fired) {
      if (f.$1.severity > worst.severity) worst = f.$1;
    }
    return ClassOutcome(c, worst, fired.map((f) => f.$2).toList(growable: false));
  }
}

/// The suitability assessment aggregate.
class SuitabilityAssessment {
  final SuitabilityId id;
  final CustomerId customerId;
  final AssessmentState state;
  final SuitabilityAnswers? answers;
  final SuitabilityDerived? derived;
  final List<ClassOutcome> outcomes;
  final DateTime? evaluatedAt;
  final DateTime? validUntil;
  final ConsentProofPresent consentProof;
  final String algorithmVersion;

  const SuitabilityAssessment({
    required this.id,
    required this.customerId,
    required this.state,
    required this.consentProof,
    this.answers,
    this.derived,
    this.outcomes = const [],
    this.evaluatedAt,
    this.validUntil,
    this.algorithmVersion = SuitabilityAlgorithm.version,
  });

  /// SUIT-R21: an assessment cannot be created without a `CNS-SOL` consent.
  /// The caller must present proof; see [ConsentProofPresent].
  factory SuitabilityAssessment.start({
    required SuitabilityId id,
    required CustomerId customerId,
    required ConsentProofPresent consentProof,
  }) =>
      SuitabilityAssessment(
        id: id,
        customerId: customerId,
        state: AssessmentState.inProgress,
        consentProof: consentProof,
      );

  /// `IN_PROGRESS -> COMPLETED`. SUIT-R01: all twelve inputs must be present,
  /// which the [SuitabilityAnswers] constructor enforces by requiring them.
  SuitabilityAssessment complete(SuitabilityAnswers a, DateTime now) {
    if (state != AssessmentState.inProgress) {
      throw StateError('INV-SUI-01: assessment is immutable once completed');
    }
    final d = SuitabilityAlgorithm.derive(a);
    return SuitabilityAssessment(
      id: id,
      customerId: customerId,
      state: AssessmentState.completed,
      consentProof: consentProof,
      answers: a,
      derived: d,
      outcomes: SuitabilityAlgorithm.evaluate(a, d),
      evaluatedAt: now,
      validUntil: now.add(SuitabilityAlgorithm.validity),
    );
  }

  ClassOutcome? outcomeFor(ProductClass c) {
    for (final o in outcomes) {
      if (o.productClass == c) return o;
    }
    return null;
  }

  /// The capability token, or `null` if this assessment does not permit a quote
  /// for [productClass].
  ///
  /// This is the single mint point for [SuitabilityEvaluationRef] in the entire
  /// application. All four SUIT-R20 conditions are checked here:
  ///
  /// 1. the assessment is `COMPLETED` (not superseded, expired or abandoned);
  /// 2. it was evaluated within the 30-day window;
  /// 3. it is bound to a customer — the caller compares against its own;
  /// 4. the outcome for the requested class permits a quote.
  SuitabilityEvaluationRef? evaluationRef(ProductClass productClass, DateTime now) {
    if (state != AssessmentState.completed) return null;
    final until = validUntil;
    if (until == null || !now.isBefore(until)) return null;
    final outcome = outcomeFor(productClass);
    if (outcome == null || !outcome.outcome.permitsQuote) return null;
    return SuitabilityEvaluationRef._(
      suitabilityId: id,
      customerId: customerId,
      productClass: productClass,
      outcome: outcome.outcome,
      validUntil: until,
    );
  }
}

/// A marker carried by an assessment recording that `CNS-SOL` consent was
/// present when it was started (SUIT-R21). Holding the consent id is enough —
/// the evidence itself is never copied across contexts (relationship R-09).
class ConsentProofPresent {
  final ConsentId consentId;
  const ConsentProofPresent(this.consentId);
}
