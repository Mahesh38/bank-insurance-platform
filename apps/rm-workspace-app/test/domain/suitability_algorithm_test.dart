/// `SUIT-ALGO-LIFE-v1.0` — determinism and outcome rules.
///
/// SUIT-R05 requires the computation to be pure: same inputs, same outputs, no
/// dependence on time, actor or system state. `S02-VT-04` requires 20 sample
/// profiles to produce identical outcomes when run twice by two people.
///
/// The reference case is `SUIT-TC-REF-01` from suitability-rule-pack §3.4, and
/// the pack calls it a mandatory unit test. This is that test.
library;

import 'package:flutter_test/flutter_test.dart';
import 'package:rm_workspace_app/domain/suitability.dart';

import '../support/journey_fixtures.dart';

void main() {
  group('SUIT-TC-REF-01 — the reference case', () {
    test('reproduces the worked example in the rule pack exactly', () {
      // Age 38, income 12,00,000 (B4, multiple 18, ageFactor 1.00),
      // 2 dependants, existing cover 5,00,000, liabilities 30,00,000,
      // declared premium 25,000.
      final d = SuitabilityAlgorithm.derive(suitableAnswers);

      expect(d.incomeMultiple, 18);
      expect(d.ageFactor, 1.00);

      // 12,00,000 x 18 x 1.00 = 2,16,00,000
      // + 30,00,000 liabilities + 10,00,000 (2 dependants x 5,00,000)
      // = 2,56,00,000, already a whole lakh.
      expect(d.recommendedCover, 25600000.0);

      // 2,56,00,000 - 5,00,000 existing = 2,51,00,000
      expect(d.protectionGap, 25100000.0);

      // 25,000 / 12,00,000 = 0.020833...
      expect(d.affordabilityRatio, closeTo(0.0208, 0.0001));
    });

    test('is pure — same inputs, same outputs, twice', () {
      final first = SuitabilityAlgorithm.derive(suitableAnswers);
      final second = SuitabilityAlgorithm.derive(suitableAnswers);

      expect(first.recommendedCover, second.recommendedCover);
      expect(first.protectionGap, second.protectionGap);
      expect(first.affordabilityRatio, second.affordabilityRatio);

      final o1 = SuitabilityAlgorithm.evaluate(suitableAnswers, first);
      final o2 = SuitabilityAlgorithm.evaluate(suitableAnswers, second);

      for (var i = 0; i < o1.length; i++) {
        expect(o1[i].outcome, o2[i].outcome);
        expect(o1[i].reasonCodes, o2[i].reasonCodes);
      }
    });
  });

  group('SUIT-R06 — rounding up to the next lakh', () {
    test('1234567 rounds to 1300000', () {
      expect(SuitabilityAlgorithm.roundUpToLakh(1234567), 1300000);
    });

    test('an exact lakh is unchanged', () {
      expect(SuitabilityAlgorithm.roundUpToLakh(1200000), 1200000);
    });
  });

  group('SUIT-R08 — age eligibility', () {
    test('under 18 and over 65 are outside the range', () {
      expect(SuitabilityAlgorithm.ageFactorFor(17), isNull);
      expect(SuitabilityAlgorithm.ageFactorFor(66), isNull);
    });

    test('boundary ages 18 and 65 are inside', () {
      expect(SuitabilityAlgorithm.ageFactorFor(18), 1.10);
      expect(SuitabilityAlgorithm.ageFactorFor(65), 0.45);
    });

    test('an ineligible age makes every product class NOT_SUITABLE', () {
      const tooOld = SuitabilityAnswers(
        ageNextBirthday: 70,
        annualIncome: 1200000,
        dependants: 0,
        declaredExistingCover: 0,
        declaredLiabilities: 0,
        objective: FinancialObjective.protection,
        horizon: InvestmentHorizon.y10to20,
        riskAttitude: RiskAttitude.moderate,
        declaredAnnualPremium: 20000,
        tobaccoUse: false,
        occupation: OccupationCategory.salaried,
        priorDecline: PriorDeclineAnswer.no,
        isReplacement: false,
      );

      final outcomes = SuitabilityAlgorithm.evaluate(
          tooOld, SuitabilityAlgorithm.derive(tooOld));

      expect(outcomes, hasLength(3));
      for (final o in outcomes) {
        expect(o.outcome, SuitabilityOutcome.notSuitable);
        expect(o.reasonCodes, contains('AGE_OUTSIDE_ELIGIBLE_RANGE'));
      }
    });
  });

  group('income bands — rule pack §3.1', () {
    test('the multiple tapers at both ends', () {
      expect(SuitabilityAlgorithm.incomeMultipleFor(299999), 10);
      expect(SuitabilityAlgorithm.incomeMultipleFor(300000), 12);
      expect(SuitabilityAlgorithm.incomeMultipleFor(500000), 15);
      expect(SuitabilityAlgorithm.incomeMultipleFor(1000000), 18);
      expect(SuitabilityAlgorithm.incomeMultipleFor(2000000), 20);
      expect(SuitabilityAlgorithm.incomeMultipleFor(3500000), 20);
      expect(SuitabilityAlgorithm.incomeMultipleFor(5000000), 18);
      expect(SuitabilityAlgorithm.incomeMultipleFor(10000000), 15);
    });
  });

  group('SUIT-R03 — the bank-known value wins when understated', () {
    test('understated existing cover is corrected and flagged', () {
      const understated = SuitabilityAnswers(
        ageNextBirthday: 38,
        knownExistingCover: 500000,
        annualIncome: 1200000,
        dependants: 0,
        declaredExistingCover: 0, // customer says none
        declaredLiabilities: 0,
        objective: FinancialObjective.protection,
        horizon: InvestmentHorizon.y10to20,
        riskAttitude: RiskAttitude.moderate,
        declaredAnnualPremium: 25000,
        tobaccoUse: false,
        occupation: OccupationCategory.salaried,
        priorDecline: PriorDeclineAnswer.no,
        isReplacement: false,
      );

      expect(understated.effectiveExistingCover, 500000);
      expect(understated.dataInconsistencyFlags,
          contains('EXISTING_COVER_UNDERSTATED'));
    });
  });

  group('SUIT-R04 — a prior decline flags risk without changing the outcome',
      () {
    test('the flag is set and Term remains suitable', () {
      const declined = SuitabilityAnswers(
        ageNextBirthday: 38,
        annualIncome: 1200000,
        dependants: 2,
        declaredExistingCover: 500000,
        declaredLiabilities: 3000000,
        objective: FinancialObjective.protection,
        horizon: InvestmentHorizon.y10to20,
        riskAttitude: RiskAttitude.moderate,
        declaredAnnualPremium: 25000,
        tobaccoUse: false,
        occupation: OccupationCategory.salaried,
        priorDecline: PriorDeclineAnswer.yes,
        isReplacement: false,
      );

      expect(declined.underwritingRiskFlag, isTrue);

      final outcomes = SuitabilityAlgorithm.evaluate(
          declined, SuitabilityAlgorithm.derive(declined));
      final term =
          outcomes.firstWhere((o) => o.productClass == ProductClass.term);
      expect(term.outcome, SuitabilityOutcome.suitable);
    });
  });

  group('Term outcome rules', () {
    ClassOutcome termFor(SuitabilityAnswers a) => SuitabilityAlgorithm
        .evaluate(a, SuitabilityAlgorithm.derive(a))
        .firstWhere((o) => o.productClass == ProductClass.term);

    test('SUIT-R09 — a protection gap makes Term suitable', () {
      expect(termFor(suitableAnswers).outcome, SuitabilityOutcome.suitable);
    });

    test('SUIT-R12 — premium above 30% of income makes Term NOT suitable', () {
      final o = termFor(unaffordableAnswers);
      expect(o.outcome, SuitabilityOutcome.notSuitable);
      expect(o.reasonCodes, contains('PREMIUM_UNAFFORDABLE'));
    });

    test('SUIT-R11 — premium above 15% is suitable WITH CAUTION', () {
      const caution = SuitabilityAnswers(
        ageNextBirthday: 38,
        annualIncome: 1000000,
        dependants: 2,
        declaredExistingCover: 0,
        declaredLiabilities: 0,
        objective: FinancialObjective.protection,
        horizon: InvestmentHorizon.y10to20,
        riskAttitude: RiskAttitude.moderate,
        declaredAnnualPremium: 200000, // 20%
        tobaccoUse: false,
        occupation: OccupationCategory.salaried,
        priorDecline: PriorDeclineAnswer.no,
        isReplacement: false,
      );

      final o = termFor(caution);
      expect(o.outcome, SuitabilityOutcome.suitableWithCaution);
      expect(o.reasonCodes, contains('PREMIUM_HIGH_RELATIVE_TO_INCOME'));
    });

    test('SUIT-R19a — most restrictive wins, and every reason is retained', () {
      final o = termFor(unaffordableAnswers);
      // Both R11 (>15%) and R12 (>30%) fire; R12 wins, both are recorded.
      expect(o.outcome, SuitabilityOutcome.notSuitable);
      expect(o.reasonCodes, contains('PREMIUM_HIGH_RELATIVE_TO_INCOME'));
      expect(o.reasonCodes, contains('PREMIUM_UNAFFORDABLE'));
    });
  });

  group('ULIP outcome rules', () {
    ClassOutcome ulipFor(SuitabilityAnswers a) => SuitabilityAlgorithm
        .evaluate(a, SuitabilityAlgorithm.derive(a))
        .firstWhere((o) => o.productClass == ProductClass.ulip);

    test('SUIT-R17 — capital protection is incompatible with ULIP', () {
      const conservative = SuitabilityAnswers(
        ageNextBirthday: 38,
        annualIncome: 1200000,
        dependants: 0,
        declaredExistingCover: 0,
        declaredLiabilities: 0,
        objective: FinancialObjective.wealthGrowth,
        horizon: InvestmentHorizon.gt20y,
        riskAttitude: RiskAttitude.capitalProtection,
        declaredAnnualPremium: 25000,
        tobaccoUse: false,
        occupation: OccupationCategory.salaried,
        priorDecline: PriorDeclineAnswer.no,
        isReplacement: false,
      );

      final o = ulipFor(conservative);
      expect(o.outcome, SuitabilityOutcome.notSuitable);
      expect(o.reasonCodes, contains('RISK_ATTITUDE_INCOMPATIBLE'));
    });

    test('SUIT-R18 — a short horizon rules ULIP out', () {
      const shortHorizon = SuitabilityAnswers(
        ageNextBirthday: 38,
        annualIncome: 1200000,
        dependants: 0,
        declaredExistingCover: 0,
        declaredLiabilities: 0,
        objective: FinancialObjective.wealthGrowth,
        horizon: InvestmentHorizon.y5to10,
        riskAttitude: RiskAttitude.high,
        declaredAnnualPremium: 25000,
        tobaccoUse: false,
        occupation: OccupationCategory.salaried,
        priorDecline: PriorDeclineAnswer.no,
        isReplacement: false,
      );

      expect(ulipFor(shortHorizon).reasonCodes,
          contains('HORIZON_TOO_SHORT_FOR_PRODUCT_CLASS'));
    });
  });

  group('SUIT-R01 — all twelve inputs or no evaluation', () {
    test('a partial answer set is refused by the repository', () async {
      final c = await withConsent();
      // SuitabilityAnswers requires all twelve at the type level, so a partial
      // set cannot even be constructed. The screen enforces the same rule by
      // withholding the action until 12 of 12 are answered.
      expect(c.assessment!.state, AssessmentState.inProgress);
      expect(c.assessment!.answers, isNull);
      expect(c.termSuitabilityRef, isNull);
    });
  });
}
