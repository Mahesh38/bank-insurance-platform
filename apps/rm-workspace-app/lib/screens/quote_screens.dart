/// SCR-10 Eligible products · SCR-11 Quote · SCR-12 Compare and select.
///
/// Every screen in this file sits behind the suitability hard-gate. Reaching
/// any of them without a valid evaluation renders `GuardBlockedScreen`, and
/// the quote request itself cannot be constructed without the capability token.
library;

import 'package:flutter/material.dart';

import '../app.dart';
import '../design/components.dart';
import '../design/tokens.dart';
import '../domain/sales.dart';
import '../guards/journey_guard.dart';
import 'customer_and_lead.dart';

/// SCR-10 — Eligible products. BR-PROD-010, AC-PROD-010-1/2.
class EligibleProductsScreen extends StatelessWidget {
  const EligibleProductsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final products = c.eligibleProducts;

    return AppScreen(
      title: 'Eligible products',
      screenId: 'SCR-10',
      steps: kJourneySteps,
      currentStep: 3,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading(
            'Products this customer is eligible for',
            subtitle:
                'Filtered by the suitability outcome first, then by product '
                'eligibility.',
          ),
          if (products.isEmpty)
            const EmptyState(
              title: 'No products match this customer.',
              explanation:
                  'The suitability outcome and the product eligibility rules '
                  'together leave nothing to offer. No quote can be requested.',
            ),
          for (final p in products)
            Padding(
              padding: const EdgeInsets.only(bottom: AppSpace.x3),
              child: AppCard(
                selected: c.selectedProduct == p,
                onTap: () async {
                  await c.requestQuote(
                    product: p,
                    sumAssured: 5000000,
                    termYears: 25,
                  );
                  if (context.mounted && c.lastError == null) {
                    go(context, AppRoute.quote);
                  }
                },
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(p.bankProductName, style: AppType.headingS),
                    const SizedBox(height: AppSpace.x1),
                    Text(p.insurerDisplayName,
                        style: AppType.caption
                            .copyWith(color: AppColor.textSecondary)),
                    const SizedBox(height: AppSpace.x3),
                    Text(
                      'Ages ${p.minAge}–${p.maxAge} · cover from '
                      '₹${p.minSumAssured.toStringAsFixed(0)}',
                      style: AppType.body,
                    ),
                  ],
                ),
              ),
            ),
          const SizedBox(height: AppSpace.x4),
          Text(
            'Insurer and product content is sample data. The R0 panel is '
            'pending commercial confirmation (S04-OPEN-01).',
            style: AppType.caption.copyWith(color: AppColor.textSecondary),
          ),
          if (c.lastError != null) ...[
            const SizedBox(height: AppSpace.x4),
            ErrorBanner(c.lastError!),
          ],
        ],
      ),
    );
  }
}

/// SCR-11 — Quote request and status. BR-QUOTE-010/020.
class QuoteScreen extends StatelessWidget {
  const QuoteScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final q = c.quote;

    return AppScreen(
      title: 'Quote',
      screenId: 'SCR-11',
      steps: kJourneySteps,
      currentStep: 4,
      footer: q != null
          ? AppButton(
              label: 'Compare offers',
              icon: Icons.arrow_forward,
              onPressed: () => go(context, AppRoute.compare),
            )
          : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading('Quote request'),
          if (q == null)
            const EmptyState(
              title: 'No quote requested yet.',
              explanation: 'Choose a product to request a quote.',
            )
          else ...[
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                          child: Text('Quote ${q.id.value}',
                              style: AppType.mono)),
                      StatusChip(
                        label: _stateLabel(q.state),
                        tone: switch (q.state) {
                          QuoteState.quoted => StatusTone.success,
                          QuoteState.partiallyQuoted => StatusTone.warning,
                          QuoteState.failed ||
                          QuoteState.timedOut ||
                          QuoteState.rejected =>
                            StatusTone.error,
                          _ => StatusTone.pending,
                        },
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpace.x3),
                  LabelledValue('Gated on evaluation',
                      q.suitabilityRef.suitabilityId.value,
                      mono: true),
                  LabelledValue('Offers received', '${q.offers.length}'),
                ],
              ),
            ),
            const SizedBox(height: AppSpace.x4),
            Text(
              'Every quote request carries the suitability evaluation it was '
              'gated on. A request without one is refused before it reaches an '
              'insurer.',
              style: AppType.caption.copyWith(color: AppColor.textSecondary),
            ),
          ],
        ],
      ),
    );
  }

  static String _stateLabel(QuoteState s) => switch (s) {
        QuoteState.requested => 'Requested',
        QuoteState.inProgress => 'Waiting for insurers',
        QuoteState.quoted => 'Quoted',
        QuoteState.partiallyQuoted => 'Partially quoted',
        QuoteState.failed => 'Failed',
        QuoteState.timedOut => 'Timed out',
        QuoteState.selected => 'Offer selected',
        QuoteState.expired => 'Expired',
        QuoteState.converted => 'Converted',
        QuoteState.rejected => 'Refused — no suitability',
      };
}

/// SCR-12 — Compare and select. BR-COMP-010, AC-COMP-010-1/2/3.
///
/// QR-06: the ranking rule is disclosed on screen. QR-07: commission is not an
/// input to the ordering, and there is no field on an offer that could make it
/// one.
class CompareScreen extends StatelessWidget {
  const CompareScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final q = c.quote!;
    final offers = q.rankedOffers;

    return AppScreen(
      title: 'Compare offers',
      screenId: 'SCR-12',
      steps: kJourneySteps,
      currentStep: 4,
      footer: q.selectedOfferId != null
          ? AppButton(
              label: 'Start the proposal',
              icon: Icons.arrow_forward,
              onPressed: () => go(context, AppRoute.proposal),
            )
          : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading('Offers'),
          Container(
            padding: const EdgeInsets.all(AppSpace.x3),
            decoration: BoxDecoration(
              color: AppColor.surfaceMuted,
              borderRadius: AppRadius.smAll,
            ),
            child: Row(
              children: [
                const Icon(Icons.sort, size: 16, color: AppColor.textSecondary),
                const SizedBox(width: AppSpace.x2),
                Expanded(
                  child: Text(
                    'Ordered by annual premium, lowest first, for the same sum '
                    'assured and term. Nothing else influences this order.',
                    style: AppType.caption
                        .copyWith(color: AppColor.textSecondary),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: AppSpace.x4),
          for (final o in offers)
            Padding(
              padding: const EdgeInsets.only(bottom: AppSpace.x3),
              child: AppCard(
                selected: q.selectedOfferId == o.id,
                onTap: () async {
                  await c.selectOffer(o.id);
                  if (context.mounted && c.lastError == null) {
                    go(context, AppRoute.proposal);
                  }
                },
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(o.bankProductName, style: AppType.headingS),
                        ),
                        Text('₹${o.annualPremium.toStringAsFixed(0)} / year',
                            style: AppType.headingS),
                      ],
                    ),
                    const SizedBox(height: AppSpace.x1),
                    Text(o.insurerDisplayName,
                        style: AppType.caption
                            .copyWith(color: AppColor.textSecondary)),
                    const SizedBox(height: AppSpace.x3),
                    LabelledValue('Sum assured',
                        '₹${o.sumAssured.toStringAsFixed(0)}'),
                    LabelledValue('Term', '${o.termYears} years'),
                    LabelledValue('Benefits', o.keyBenefits.join(', ')),
                    LabelledValue('Offer valid until',
                        o.validUntil.toIso8601String().substring(0, 10)),
                  ],
                ),
              ),
            ),
          if (c.lastError != null) ...[
            const SizedBox(height: AppSpace.x4),
            ErrorBanner(c.lastError!),
          ],
        ],
      ),
    );
  }
}
