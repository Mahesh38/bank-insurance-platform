/// SCR-17 — Policy confirmation.
///
/// "Sold" is shown only when all four conditions hold: issued, confirmed,
/// reconciled and persisted in the bank's audit store. `Policy.isSold` is a
/// derived getter, never a settable flag — `AC-POL-010-2`, D-007, INV-JRN-05.
///
/// This screen is the capability the legacy AU Beema Portal lacks entirely:
/// after the sale, the bank can still see what happened.
library;

import 'package:flutter/material.dart';

import '../app.dart';
import '../design/components.dart';
import '../design/tokens.dart';
import '../guards/journey_guard.dart';
import 'customer_and_lead.dart';

class PolicyConfirmationScreen extends StatelessWidget {
  const PolicyConfirmationScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final p = c.policy!;

    return AppScreen(
      title: 'Policy',
      screenId: 'SCR-17',
      steps: kJourneySteps,
      currentStep: 7,
      footer: AppButton(
        label: 'Back to pipeline',
        icon: Icons.list_alt,
        onPressed: () => go(context, AppRoute.pipeline),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading('Issued'),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(p.policyNumber ?? '—', style: AppType.mono),
                    ),
                    StatusChip(
                      label: p.isSold ? 'Sold' : 'Not yet sold',
                      tone: p.isSold ? StatusTone.success : StatusTone.warning,
                    ),
                  ],
                ),
                const SizedBox(height: AppSpace.x4),
                LabelledValue('Customer', c.customer!.fullName),
                LabelledValue(
                    'Product', c.quote!.selectedOffer!.bankProductName),
                LabelledValue(
                    'Insurer', c.quote!.selectedOffer!.insurerDisplayName),
              ],
            ),
          ),
          const SizedBox(height: AppSpace.x5),
          Text('What "sold" requires', style: AppType.headingS),
          const SizedBox(height: AppSpace.x2),
          Text(
            'All four must be true. Three of four is not a sale.',
            style: AppType.body.copyWith(color: AppColor.textSecondary),
          ),
          const SizedBox(height: AppSpace.x4),
          _condition('Policy issued by the insurer', p.policyNumber != null),
          _condition('Issuance confirmed by the insurer', p.issuanceConfirmed),
          _condition('Premium reconciled against the gateway',
              p.paymentReconciled),
          _condition('Record persisted in the bank audit store',
              p.auditPersisted),
          const SizedBox(height: AppSpace.x5),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Evidence trail', style: AppType.headingS),
                const SizedBox(height: AppSpace.x3),
                LabelledValue('Suitability evaluation', c.assessment!.id.value,
                    mono: true),
                LabelledValue('Quote', c.quote!.id.value, mono: true),
                LabelledValue('Proposal', c.proposal!.id.value, mono: true),
                LabelledValue('Payment', c.payment!.id.value, mono: true),
                LabelledValue('Policy', p.id.value, mono: true),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _condition(String label, bool met) => Padding(
        padding: const EdgeInsets.only(bottom: AppSpace.x2),
        child: Row(
          children: [
            Icon(
              met ? Icons.check_circle_outline : Icons.radio_button_unchecked,
              size: 20,
              color: met ? AppColor.statusSuccess : AppColor.textSecondary,
            ),
            const SizedBox(width: AppSpace.x3),
            Expanded(child: Text(label, style: AppType.body)),
          ],
        ),
      );
}
