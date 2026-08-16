/// SCR-01 Login · SCR-02 RM pipeline · SCR-18 Pilot funnel.
library;

import 'package:flutter/material.dart';

import '../app.dart';
import '../design/components.dart';
import '../design/tokens.dart';
import '../domain/journey.dart';
import '../guards/journey_guard.dart';

/// SCR-01 — Login / SSO landing. BR-SEC-010, AC-SEC-010-1.
class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    return AppScreen(
      title: 'AU Bank — RM Workspace',
      screenId: 'SCR-01',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading(
            'Sign in',
            subtitle:
                'Bank single sign-on. In R0 this is a demonstration sign-in; '
                'the real path federates with Bank AD through the workforce BFF '
                '(WS-2), and the RM app never receives an OAuth token.',
          ),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const LabelledValue('Relationship Manager', 'Demo RM'),
                const LabelledValue('IRDAI SP licence', 'SP-DEMO-778812', mono: true),
                const LabelledValue('Branch', 'Demo branch'),
                const SizedBox(height: AppSpace.x4),
                AppButton(
                  label: 'Sign in with Bank SSO',
                  icon: Icons.login,
                  onPressed: () {
                    c.signIn();
                    goReplace(context, AppRoute.pipeline);
                  },
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// SCR-02 — RM pipeline. BR-RM-010, BR-LEAD-030, AC-LEAD-030-1.
class PipelineScreen extends StatelessWidget {
  const PipelineScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final hasJourney = c.lead != null;

    return AppScreen(
      title: 'My pipeline',
      screenId: 'SCR-02',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading('Journeys',
              subtitle: 'Your leads and where each one has reached.'),
          if (!hasJourney)
            EmptyState(
              title: 'No journeys yet.',
              explanation:
                  'Start with a customer search. R0 covers existing AU Bank '
                  'customers buying Term Life through an RM-assisted journey.',
              action: AppButton(
                label: 'Search for a customer',
                icon: Icons.search,
                onPressed: () => go(context, AppRoute.customerSearch),
              ),
            )
          else ...[
            AppCard(
              onTap: () => go(context, _resumeRouteFor(c.stage)),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(c.customer?.fullName ?? 'Customer',
                            style: AppType.headingS),
                      ),
                      StatusChip(
                        label: stageLabel(c.stage),
                        tone: c.stage == JourneyStage.sold
                            ? StatusTone.success
                            : StatusTone.pending,
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpace.x3),
                  LabelledValue('Lead', c.lead!.id.value, mono: true),
                  LabelledValue('Journey stage', stageLabel(c.stage)),
                  const SizedBox(height: AppSpace.x2),
                  Text('Tap to resume at the last incomplete step.',
                      style: AppType.caption
                          .copyWith(color: AppColor.textSecondary)),
                ],
              ),
            ),
            const SizedBox(height: AppSpace.x5),
            AppButton(
              label: 'Start another journey',
              variant: AppButtonVariant.secondary,
              icon: Icons.add,
              onPressed: () {
                c.reset();
                go(context, AppRoute.customerSearch);
              },
            ),
          ],
          const SizedBox(height: AppSpace.x6),
          AppButton(
            label: 'Pilot funnel',
            variant: AppButtonVariant.tertiary,
            icon: Icons.insights_outlined,
            onPressed: () => go(context, AppRoute.funnel),
          ),
        ],
      ),
    );
  }

  /// AC-LEAD-020-1: resume lands on the last incomplete step.
  AppRoute _resumeRouteFor(JourneyStage stage) => switch (stage) {
        JourneyStage.initiated => AppRoute.customerSearch,
        JourneyStage.needAnalysis => AppRoute.consent,
        JourneyStage.suitabilityComplete => AppRoute.suitabilityOutcome,
        JourneyStage.consentCaptured => AppRoute.products,
        JourneyStage.quoting => AppRoute.quote,
        JourneyStage.quoteSelected => AppRoute.proposal,
        JourneyStage.proposalInProgress => AppRoute.submit,
        JourneyStage.underwriting => AppRoute.status,
        JourneyStage.paymentPending || JourneyStage.paymentSettled => AppRoute.payment,
        _ => AppRoute.confirmation,
      };
}

/// SCR-18 — Pilot funnel. BR-REP-010, AC-REP-010-1. Ops/PO surface, not RM.
class FunnelScreen extends StatelessWidget {
  const FunnelScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final reached = kHappyPath.indexOf(c.stage);

    return AppScreen(
      title: 'Pilot funnel',
      screenId: 'SCR-18',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading(
            'Funnel — this session',
            subtitle:
                'KPI-02 stage-to-stage conversion. In R0 this counts the '
                'in-memory demo journey only; the real funnel reads journey '
                'orchestration events.',
          ),
          for (var i = 0; i < kHappyPath.length; i++)
            Padding(
              padding: const EdgeInsets.only(bottom: AppSpace.x2),
              child: Row(
                children: [
                  SizedBox(
                    width: 220,
                    child: Text(stageLabel(kHappyPath[i]), style: AppType.body),
                  ),
                  StatusChip(
                    label: (reached >= i && reached >= 0) ? '1' : '0',
                    tone: (reached >= i && reached >= 0)
                        ? StatusTone.success
                        : StatusTone.pending,
                  ),
                ],
              ),
            ),
          const SizedBox(height: AppSpace.x5),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Control KPIs', style: AppType.headingS),
                const SizedBox(height: AppSpace.x3),
                const LabelledValue('KPI-03 suitability bypass rate', '0'),
                const LabelledValue('KPI-04 payments on RM device', '0'),
                const SizedBox(height: AppSpace.x2),
                Text(
                  'Both are constants, not trends. Any non-zero value halts the '
                  'pilot.',
                  style: AppType.caption.copyWith(color: AppColor.textSecondary),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
