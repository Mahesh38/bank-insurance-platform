/// SCR-13 Proposal form · SCR-14 Review and submit · SCR-15 Application status.
library;

import 'package:flutter/material.dart';

import '../app.dart';
import '../design/components.dart';
import '../design/tokens.dart';
import '../domain/consent.dart';
import '../domain/sales.dart';
import '../guards/journey_guard.dart';
import 'customer_and_lead.dart';

/// SCR-13 — Proposal form. BR-PROP-010/020.
///
/// Reaching this screen requires the insurer-sharing consent (CNS-SHR). That
/// consent is captured here as a separate interaction, because the counterparty
/// is named in the statement — the customer must see which insurer their data
/// goes to.
class ProposalFormScreen extends StatefulWidget {
  const ProposalFormScreen({super.key});

  @override
  State<ProposalFormScreen> createState() => _ProposalFormScreenState();
}

class _ProposalFormScreenState extends State<ProposalFormScreen> {
  final _answers = <String, String>{};
  bool _sharingRequested = false;

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final hasSharing = c.hasSharingConsent;
    final offer = c.quote?.selectedOffer;

    // The sharing consent gate, rendered rather than routed: the RM arrives
    // here from compare, and this is where the consent is captured.
    if (!hasSharing) {
      return AppScreen(
        title: 'Consent to share with the insurer',
        screenId: 'SCR-13',
        steps: kJourneySteps,
        currentStep: 5,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SectionHeading(
              'Before the proposal',
              subtitle:
                  'The customer must consent to their details being shared with '
                  '${offer?.insurerDisplayName ?? 'the selected insurer'}. '
                  'This consent names the insurer, so it is always captured '
                  'separately.',
            ),
            if (!_sharingRequested)
              AppButton(
                label: 'Send sharing consent to the customer',
                icon: Icons.send,
                loading: c.busy,
                onPressed: () async {
                  setState(() => _sharingRequested = true);
                  await c.captureSharingConsent();
                  if (mounted) setState(() {});
                },
              )
            else
              WaitingPanel(
                title: 'Waiting for the customer to confirm sharing',
                maskedDestination: c.customer!.maskedMobile.lastTwo,
                explanation:
                    'The customer confirms on their own phone. You cannot '
                    'confirm on their behalf.',
              ),
            if (c.lastError != null) ...[
              const SizedBox(height: AppSpace.x4),
              ErrorBanner(c.lastError!),
            ],
          ],
        ),
      );
    }

    return AppScreen(
      title: 'Proposal',
      screenId: 'SCR-13',
      steps: kJourneySteps,
      currentStep: 5,
      footer: AppButton(
        label: 'Review and submit',
        icon: Icons.arrow_forward,
        loading: c.busy,
        onPressed: () async {
          if (c.proposal == null) await c.createProposal();
          await c.saveProposalAnswers(_answers);
          if (context.mounted && c.lastError == null) {
            go(context, AppRoute.submit);
          }
        },
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading(
            'Proposal details',
            subtitle:
                'The form is rendered from the insurer schema. Answers save as '
                'you go and can be resumed from the pipeline.',
          ),
          _field('Occupation detail', 'occupation'),
          _field('Height (cm)', 'heightCm'),
          _field('Weight (kg)', 'weightKg'),
          _field('Nominee name', 'nomineeName'),
          _field('Nominee relationship', 'nomineeRelationship'),
          const SizedBox(height: AppSpace.x4),
          AppCard(
            child: Row(
              children: [
                const Icon(Icons.verified_user_outlined,
                    color: AppColor.statusSuccess),
                const SizedBox(width: AppSpace.x3),
                Expanded(
                  child: Text(
                    'Sharing consent captured for '
                    '${offer?.insurerDisplayName ?? 'the selected insurer'}.',
                    style: AppType.body,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _field(String label, String key) => Padding(
        padding: const EdgeInsets.only(bottom: AppSpace.x4),
        child: TextField(
          decoration:
              InputDecoration(labelText: label, border: const OutlineInputBorder()),
          onChanged: (v) => _answers[key] = v,
        ),
      );
}

/// SCR-14 — Review and submit. BR-PROP-030, AC-PROP-030-1..4.
class ProposalSubmitScreen extends StatelessWidget {
  const ProposalSubmitScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final p = c.proposal;
    final offer = c.quote!.selectedOffer!;

    return AppScreen(
      title: 'Review and submit',
      screenId: 'SCR-14',
      steps: kJourneySteps,
      currentStep: 5,
      footer: AppButton(
        label: 'Submit to the insurer',
        icon: Icons.send,
        loading: c.busy,
        onPressed: (p != null && p.state == ProposalState.draft)
            ? () async {
                await c.submitProposal();
                if (context.mounted && c.lastError == null) {
                  go(context, AppRoute.status);
                }
              }
            : null,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading('Check before submitting'),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LabelledValue('Customer', c.customer!.fullName),
                LabelledValue('Product', offer.bankProductName),
                LabelledValue('Insurer', offer.insurerDisplayName),
                LabelledValue('Annual premium',
                    '₹${offer.annualPremium.toStringAsFixed(0)}'),
                LabelledValue('Sum assured',
                    '₹${offer.sumAssured.toStringAsFixed(0)}'),
              ],
            ),
          ),
          const SizedBox(height: AppSpace.x4),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Attribution and evidence', style: AppType.headingS),
                const SizedBox(height: AppSpace.x3),
                const LabelledValue('SP licence', 'SP-DEMO-778812', mono: true),
                const LabelledValue('Distributor ID',
                    'Injected server-side — never from this device'),
                LabelledValue(
                  'Sharing consent',
                  c.consentRef(ConsentCode.sharing)?.consentId.value ?? '—',
                  mono: true,
                ),
                LabelledValue(
                  'Suitability evaluation',
                  c.assessment!.id.value,
                  mono: true,
                ),
              ],
            ),
          ),
          if (p != null && p.state != ProposalState.draft) ...[
            const SizedBox(height: AppSpace.x4),
            AppCard(
              child: Row(
                children: [
                  const Icon(Icons.check_circle_outline,
                      color: AppColor.statusSuccess),
                  const SizedBox(width: AppSpace.x3),
                  Expanded(
                    child: Text(
                        'Submitted. Application '
                        '${p.insurerApplicationRef ?? ''}',
                        style: AppType.body),
                  ),
                ],
              ),
            ),
          ],
          if (c.lastError != null) ...[
            const SizedBox(height: AppSpace.x4),
            ErrorBanner(c.lastError!),
          ],
        ],
      ),
    );
  }
}

/// SCR-15 — Application status and underwriting. BR-UW-010.
class ApplicationStatusScreen extends StatelessWidget {
  const ApplicationStatusScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final p = c.proposal!;
    final awaitingPayment = p.state == ProposalState.awaitingPayment;

    return AppScreen(
      title: 'Application status',
      screenId: 'SCR-15',
      steps: kJourneySteps,
      currentStep: 6,
      footer: awaitingPayment
          ? AppButton(
              label: 'Continue to payment',
              icon: Icons.arrow_forward,
              onPressed: () => go(context, AppRoute.payment),
            )
          : AppButton(
              label: 'Refresh status',
              icon: Icons.refresh,
              loading: c.busy,
              onPressed: () async => c.refreshUnderwriting(),
            ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading('Underwriting'),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(p.insurerApplicationRef ?? 'Not submitted',
                          style: AppType.mono),
                    ),
                    StatusChip(
                      label: _label(p.state),
                      tone: switch (p.state) {
                        ProposalState.uwApproved ||
                        ProposalState.awaitingPayment =>
                          StatusTone.success,
                        ProposalState.uwDeclined ||
                        ProposalState.submissionFailed =>
                          StatusTone.error,
                        _ => StatusTone.pending,
                      },
                    ),
                  ],
                ),
                const SizedBox(height: AppSpace.x3),
                Text(
                  'Bank status is shown alongside the insurer substatus. The '
                  'insurer reason is preserved exactly as received.',
                  style: AppType.caption
                      .copyWith(color: AppColor.textSecondary),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  static String _label(ProposalState s) => switch (s) {
        ProposalState.draft => 'Draft',
        ProposalState.submitted => 'Submitted',
        ProposalState.underWriting => 'Under writing',
        ProposalState.uwApproved => 'Approved',
        ProposalState.uwDeclined => 'Declined',
        ProposalState.awaitingPayment => 'Awaiting payment',
        ProposalState.paid => 'Paid',
        ProposalState.converted => 'Converted',
        ProposalState.issuanceFailed => 'Issuance failed',
        ProposalState.submissionFailed => 'Submission failed',
      };
}
