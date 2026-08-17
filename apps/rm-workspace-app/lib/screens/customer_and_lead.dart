/// SCR-03 Customer search · SCR-04 Customer detail · SCR-05 Lead create ·
/// SCR-07 Prefill review.
library;

import 'package:flutter/material.dart';

import '../app.dart';
import '../design/components.dart';
import '../design/tokens.dart';
import '../domain/sales.dart';
import '../guards/journey_guard.dart';

const kJourneySteps = [
  'Customer',
  'Consent',
  'Suitability',
  'Products',
  'Quote',
  'Proposal',
  'Payment',
  'Policy',
];

/// SCR-03 — Customer search. BR-CUST-010, AC-CUST-010-1/2/3.
class CustomerSearchScreen extends StatefulWidget {
  const CustomerSearchScreen({super.key});

  @override
  State<CustomerSearchScreen> createState() => _CustomerSearchScreenState();
}

class _CustomerSearchScreenState extends State<CustomerSearchScreen> {
  final _controller = TextEditingController();
  List<CustomerProfile>? _results;
  bool _searching = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    final c = JourneyScope.of(context);
    setState(() => _searching = true);
    final r = await c.searchCustomers(_controller.text);
    if (!mounted) return;
    setState(() {
      _results = r;
      _searching = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    return AppScreen(
      title: 'Find the customer',
      screenId: 'SCR-03',
      steps: kJourneySteps,
      currentStep: 0,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading(
            'Customer search',
            subtitle:
                'Search by CIF, registered mobile or PAN. R0 covers existing '
                'AU Bank customers only.',
          ),
          TextField(
            controller: _controller,
            decoration: const InputDecoration(
              labelText: 'CIF, registered mobile or PAN',
              helperText: 'Try: AU100 or a customer name',
              border: OutlineInputBorder(),
            ),
            onSubmitted: (_) => _search(),
          ),
          const SizedBox(height: AppSpace.x4),
          AppButton(
            label: 'Search',
            icon: Icons.search,
            loading: _searching,
            onPressed: _search,
          ),
          const SizedBox(height: AppSpace.x5),
          if (_results != null && _results!.isEmpty)
            const EmptyState(
              title: 'No customer found.',
              explanation:
                  'Check the CIF, registered mobile or PAN. No customer record '
                  'is created from a search, and a journey cannot start without '
                  'an existing customer.',
            ),
          if (_results != null)
            for (final r in _results!)
              Padding(
                padding: const EdgeInsets.only(bottom: AppSpace.x3),
                child: AppCard(
                  onTap: () async {
                    await c.selectCustomer(r);
                    if (context.mounted) go(context, AppRoute.customerDetail);
                  },
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(r.fullName, style: AppType.headingS),
                            const SizedBox(height: AppSpace.x1),
                            Text('CIF ${r.cif} · mobile ${r.maskedMobile}',
                                style: AppType.caption
                                    .copyWith(color: AppColor.textSecondary)),
                          ],
                        ),
                      ),
                      StatusChip(
                        label: r.isEtb ? 'Existing customer' : 'Not eligible',
                        tone: r.isEtb ? StatusTone.success : StatusTone.warning,
                      ),
                    ],
                  ),
                ),
              ),
        ],
      ),
    );
  }
}

/// SCR-04 — Customer detail and confirm. BR-CUST-010/020.
class CustomerDetailScreen extends StatelessWidget {
  const CustomerDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final cust = c.customer!;
    return AppScreen(
      title: 'Confirm the customer',
      screenId: 'SCR-04',
      steps: kJourneySteps,
      currentStep: 0,
      footer: AppButton(
        label: 'Continue',
        icon: Icons.arrow_forward,
        onPressed: cust.isEtb ? () => go(context, AppRoute.leadCreate) : null,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading('Customer'),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LabelledValue('Name', cust.fullName),
                LabelledValue('CIF', cust.cif, mono: true),
                LabelledValue('Age next birthday', '${cust.ageNextBirthday}'),
                LabelledValue('Registered mobile', cust.maskedMobile.toString()),
                LabelledValue(
                    'Relationships', cust.etbRelationshipTypes.join(', ')),
              ],
            ),
          ),
          if (!cust.isEtb) ...[
            const SizedBox(height: AppSpace.x4),
            const ErrorBanner(
              'This customer has no existing AU Bank relationship. R0 covers '
              'existing customers only, so a journey cannot start.',
            ),
          ],
          const SizedBox(height: AppSpace.x4),
          Text(
            'Only a masked mobile is held in this workspace. Full contact '
            'details stay in core banking.',
            style: AppType.caption.copyWith(color: AppColor.textSecondary),
          ),
        ],
      ),
    );
  }
}

/// SCR-05 — Lead create. BR-LEAD-010, AC-LEAD-010-1.
class LeadCreateScreen extends StatelessWidget {
  const LeadCreateScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    return AppScreen(
      title: 'Start the journey',
      screenId: 'SCR-05',
      steps: kJourneySteps,
      currentStep: 0,
      footer: AppButton(
        label: 'Create lead and continue',
        icon: Icons.arrow_forward,
        loading: c.busy,
        onPressed: () async {
          await c.createLead();
          if (context.mounted && c.lastError == null) {
            go(context, AppRoute.consent);
          }
        },
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading(
            'New lead',
            subtitle:
                'R0 covers Life — Term. Other lines of business and product '
                'classes arrive in later releases.',
          ),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LabelledValue('Customer', c.customer!.fullName),
                const LabelledValue('Line of business', 'Life'),
                const LabelledValue('Product class', 'Term'),
                const LabelledValue('Channel', 'RM-assisted'),
                const LabelledValue('Attributing SP licence', 'SP-DEMO-778812',
                    mono: true),
              ],
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

/// SCR-07 — Prefill review. BR-CUST-020, AC-CUST-020-1/3.
class PrefillReviewScreen extends StatelessWidget {
  const PrefillReviewScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final cust = c.customer!;
    return AppScreen(
      title: 'Check the details',
      screenId: 'SCR-07',
      steps: kJourneySteps,
      currentStep: 1,
      footer: AppButton(
        label: 'Continue to need analysis',
        icon: Icons.arrow_forward,
        onPressed: () async {
          await c.startSuitability();
          if (context.mounted && c.lastError == null) {
            go(context, AppRoute.suitability);
          }
        },
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading(
            'Pre-filled from core banking',
            subtitle:
                'These values came from the bank record. Corrections are kept '
                'alongside the original, never over it.',
          ),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LabelledValue('Name', cust.fullName),
                LabelledValue('CIF', cust.cif, mono: true),
                LabelledValue('Age next birthday', '${cust.ageNextBirthday}'),
                LabelledValue('Prefill recorded at',
                    cust.prefillAt.toIso8601String().substring(0, 16)),
              ],
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
