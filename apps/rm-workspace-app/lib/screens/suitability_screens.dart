/// SCR-08 Need analysis and suitability questionnaire · SCR-09 Outcome.
///
/// SUIT-R01: all twelve inputs are mandatory. There is no "skip" affordance on
/// this screen and no partial submission — the Continue action is unavailable
/// until every input is answered, and the repository refuses a partial set
/// regardless of what the UI does.
library;

import 'package:flutter/material.dart';

import '../app.dart';
import '../design/components.dart';
import '../design/tokens.dart';
import '../domain/suitability.dart';
import '../guards/journey_guard.dart';
import 'customer_and_lead.dart';

class SuitabilityQuestionnaireScreen extends StatefulWidget {
  const SuitabilityQuestionnaireScreen({super.key});

  @override
  State<SuitabilityQuestionnaireScreen> createState() =>
      _SuitabilityQuestionnaireScreenState();
}

class _SuitabilityQuestionnaireScreenState
    extends State<SuitabilityQuestionnaireScreen> {
  double? _income;
  int? _dependants;
  double? _existingCover;
  double? _liabilities;
  FinancialObjective? _objective;
  InvestmentHorizon? _horizon;
  RiskAttitude? _risk;
  double? _premium;
  bool? _tobacco;
  OccupationCategory? _occupation;
  PriorDeclineAnswer? _decline;
  bool? _replacement;

  int get _answered => [
        _income,
        _dependants,
        _existingCover,
        _liabilities,
        _objective,
        _horizon,
        _risk,
        _premium,
        _tobacco,
        _occupation,
        _decline,
        _replacement,
      ].where((v) => v != null).length;

  bool get _complete => _answered == 12;

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final cust = c.customer!;

    return AppScreen(
      title: 'Need analysis',
      screenId: 'SCR-08',
      steps: kJourneySteps,
      currentStep: 2,
      footer: Row(
        children: [
          Expanded(
            child: Text('$_answered of 12 answered',
                style: AppType.bodyStrong.copyWith(
                    color: _complete
                        ? AppColor.statusSuccess
                        : AppColor.textSecondary)),
          ),
          AppButton(
            label: 'Evaluate suitability',
            icon: Icons.calculate_outlined,
            loading: c.busy,
            // SUIT-R01 in the UI. The repository enforces it independently.
            onPressed: _complete
                ? () async {
                    await c.completeSuitability(SuitabilityAnswers(
                      ageNextBirthday: cust.ageNextBirthday,
                      annualIncome: _income!,
                      dependants: _dependants!,
                      declaredExistingCover: _existingCover!,
                      declaredLiabilities: _liabilities!,
                      objective: _objective!,
                      horizon: _horizon!,
                      riskAttitude: _risk!,
                      declaredAnnualPremium: _premium!,
                      tobaccoUse: _tobacco!,
                      occupation: _occupation!,
                      priorDecline: _decline!,
                      isReplacement: _replacement!,
                    ));
                    if (context.mounted && c.lastError == null) {
                      go(context, AppRoute.suitabilityOutcome);
                    }
                  }
                : null,
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading(
            'Need analysis and suitability',
            subtitle:
                'Every question is required. This assessment is what makes a '
                'quote lawful, and it cannot be skipped.',
          ),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('From the bank record', style: AppType.headingS),
                const SizedBox(height: AppSpace.x3),
                LabelledValue(
                    'Age next birthday', '${cust.ageNextBirthday}'),
                const LabelledValue('Source', 'Core banking — not editable'),
              ],
            ),
          ),
          const SizedBox(height: AppSpace.x5),
          _money('Annual income', (v) => setState(() => _income = v)),
          _int('Financially dependent persons',
              (v) => setState(() => _dependants = v)),
          _money('Existing life cover, all insurers',
              (v) => setState(() => _existingCover = v)),
          _money('Total outstanding liabilities',
              (v) => setState(() => _liabilities = v)),
          _choice<FinancialObjective>(
            'Primary financial objective',
            FinancialObjective.values,
            _objective,
            (v) => setState(() => _objective = v),
            _objectiveLabel,
          ),
          _choice<InvestmentHorizon>(
            'Investment horizon',
            InvestmentHorizon.values,
            _horizon,
            (v) => setState(() => _horizon = v),
            _horizonLabel,
          ),
          _choice<RiskAttitude>(
            'Attitude to investment risk',
            RiskAttitude.values,
            _risk,
            (v) => setState(() => _risk = v),
            _riskLabel,
          ),
          _money('Annual premium the customer is comfortable with',
              (v) => setState(() => _premium = v)),
          _choice<bool>(
            'Tobacco use in the last 12 months',
            const [true, false],
            _tobacco,
            (v) => setState(() => _tobacco = v),
            (v) => v ? 'Yes' : 'No',
          ),
          _choice<OccupationCategory>(
            'Occupation category',
            OccupationCategory.values,
            _occupation,
            (v) => setState(() => _occupation = v),
            (v) => v.name,
          ),
          _choice<PriorDeclineAnswer>(
            'Declined or rated by any life insurer before',
            PriorDeclineAnswer.values,
            _decline,
            (v) => setState(() => _decline = v),
            (v) => switch (v) {
              PriorDeclineAnswer.no => 'No',
              PriorDeclineAnswer.yes => 'Yes',
              PriorDeclineAnswer.dontKnow => 'Do not know',
            },
          ),
          _choice<bool>(
            'Replacing an existing policy',
            const [true, false],
            _replacement,
            (v) => setState(() => _replacement = v),
            (v) => v ? 'Yes' : 'No',
          ),
          if (c.lastError != null) ...[
            const SizedBox(height: AppSpace.x4),
            ErrorBanner(c.lastError!),
          ],
        ],
      ),
    );
  }

  Widget _money(String label, ValueChanged<double?> onChanged) => Padding(
        padding: const EdgeInsets.only(bottom: AppSpace.x4),
        child: TextField(
          keyboardType: TextInputType.number,
          decoration: InputDecoration(
              labelText: label, prefixText: '₹ ', border: const OutlineInputBorder()),
          onChanged: (v) => onChanged(double.tryParse(v)),
        ),
      );

  Widget _int(String label, ValueChanged<int?> onChanged) => Padding(
        padding: const EdgeInsets.only(bottom: AppSpace.x4),
        child: TextField(
          keyboardType: TextInputType.number,
          decoration:
              InputDecoration(labelText: label, border: const OutlineInputBorder()),
          onChanged: (v) => onChanged(int.tryParse(v)),
        ),
      );

  Widget _choice<T>(
    String label,
    List<T> options,
    T? value,
    ValueChanged<T?> onChanged,
    String Function(T) labeller,
  ) =>
      Padding(
        padding: const EdgeInsets.only(bottom: AppSpace.x4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: AppType.label),
            const SizedBox(height: AppSpace.x2),
            Wrap(
              spacing: AppSpace.x2,
              runSpacing: AppSpace.x2,
              children: [
                for (final o in options)
                  ChoiceChip(
                    label: Text(labeller(o), style: AppType.label),
                    selected: value == o,
                    onSelected: (_) => onChanged(o),
                  ),
              ],
            ),
          ],
        ),
      );

  static String _objectiveLabel(FinancialObjective o) => switch (o) {
        FinancialObjective.protection => 'Protection',
        FinancialObjective.savingsGoal => 'Savings goal',
        FinancialObjective.retirement => 'Retirement',
        FinancialObjective.wealthGrowth => 'Wealth growth',
        FinancialObjective.taxPlanning => 'Tax planning',
      };

  static String _horizonLabel(InvestmentHorizon h) => switch (h) {
        InvestmentHorizon.lt5y => 'Under 5 years',
        InvestmentHorizon.y5to10 => '5–10 years',
        InvestmentHorizon.y10to20 => '10–20 years',
        InvestmentHorizon.gt20y => 'Over 20 years',
      };

  static String _riskLabel(RiskAttitude r) => switch (r) {
        RiskAttitude.capitalProtection => 'Capital protection',
        RiskAttitude.low => 'Low',
        RiskAttitude.moderate => 'Moderate',
        RiskAttitude.high => 'High',
      };
}

/// SCR-09 — Suitability outcome. BR-SUIT-020/030.
///
/// A `NOT_SUITABLE` outcome terminates the journey for that class. SUIT-R38:
/// there is no override control on this screen, no approval workflow behind it,
/// and no configuration that produces one.
class SuitabilityOutcomeScreen extends StatelessWidget {
  const SuitabilityOutcomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final a = c.assessment!;
    final d = a.derived!;
    final termOutcome = a.outcomeFor(ProductClass.term);
    final permitted = c.hasQuotePermission;

    return AppScreen(
      title: 'Suitability outcome',
      screenId: 'SCR-09',
      steps: kJourneySteps,
      currentStep: 2,
      footer: permitted
          ? AppButton(
              label: 'See eligible products',
              icon: Icons.arrow_forward,
              onPressed: () async {
                await c.loadEligibleProducts();
                if (context.mounted && c.lastError == null) {
                  go(context, AppRoute.products);
                }
              },
            )
          : AppButton(
              label: 'Return to pipeline',
              variant: AppButtonVariant.secondary,
              onPressed: () => go(context, AppRoute.pipeline),
            ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading('Assessment result'),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LabelledValue('Evaluation id', a.id.value, mono: true),
                LabelledValue('Algorithm', a.algorithmVersion, mono: true),
                LabelledValue('Recommended cover',
                    '₹${d.recommendedCover.toStringAsFixed(0)}'),
                LabelledValue('Protection gap',
                    '₹${d.protectionGap.toStringAsFixed(0)}'),
                LabelledValue('Premium as share of income',
                    '${(d.affordabilityRatio * 100).toStringAsFixed(1)}%'),
                LabelledValue(
                    'Valid until',
                    a.validUntil!.toIso8601String().substring(0, 10)),
              ],
            ),
          ),
          const SizedBox(height: AppSpace.x5),
          const Text('Outcome by product class', style: AppType.headingS),
          const SizedBox(height: AppSpace.x3),
          for (final o in a.outcomes)
            Padding(
              padding: const EdgeInsets.only(bottom: AppSpace.x3),
              child: AppCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                            child: Text(_className(o.productClass),
                                style: AppType.bodyStrong)),
                        StatusChip(
                          label: _outcomeLabel(o.outcome),
                          tone: switch (o.outcome) {
                            SuitabilityOutcome.suitable => StatusTone.success,
                            SuitabilityOutcome.suitableWithCaution =>
                              StatusTone.warning,
                            _ => StatusTone.error,
                          },
                        ),
                      ],
                    ),
                    const SizedBox(height: AppSpace.x2),
                    Text(o.reasonCodes.join(' · '),
                        style: AppType.caption
                            .copyWith(color: AppColor.textSecondary)),
                  ],
                ),
              ),
            ),
          if (termOutcome != null && !termOutcome.outcome.permitsQuote) ...[
            const SizedBox(height: AppSpace.x4),
            const ErrorBanner(
              'Term is not suitable for this customer on these answers, so no '
              'quote can be requested. This outcome cannot be overridden by any '
              'user at any level.',
            ),
          ],
          if (permitted &&
              termOutcome?.outcome == SuitabilityOutcome.suitableWithCaution) ...[
            const SizedBox(height: AppSpace.x4),
            Container(
              padding: const EdgeInsets.all(AppSpace.x4),
              decoration: BoxDecoration(
                color: AppColor.statusWarning.withOpacity(0.06),
                borderRadius: AppRadius.mdAll,
                border: Border.all(
                    color: AppColor.statusWarning.withOpacity(0.4)),
              ),
              child: Text(
                'Proceeding requires a caution disclosure the customer '
                'acknowledges on their own device. The disclosure wording is '
                'pending Compliance approval (S05-OPEN-03).',
                style: AppType.body.copyWith(color: AppColor.statusWarning),
              ),
            ),
          ],
        ],
      ),
    );
  }

  static String _className(ProductClass c) => switch (c) {
        ProductClass.term => 'Term',
        ProductClass.savingsEndowment => 'Savings / Endowment',
        ProductClass.ulip => 'ULIP',
      };

  static String _outcomeLabel(SuitabilityOutcome o) => switch (o) {
        SuitabilityOutcome.suitable => 'Suitable',
        SuitabilityOutcome.suitableWithCaution => 'Suitable with caution',
        SuitabilityOutcome.notSuitable => 'Not suitable',
        SuitabilityOutcome.insufficientData => 'Insufficient data',
      };
}
