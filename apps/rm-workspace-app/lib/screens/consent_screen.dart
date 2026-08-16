/// SCR-06 — Consent capture, RM view.
///
/// The customer-device half (SCR-06c) is an SMS carrying the statement text and
/// an OTP; it is not a screen in this application by design.
///
/// **CNS-R13 is visible in this file as an absence.** There is no OTP entry
/// field, because the RM must not be able to complete the customer's
/// acknowledgement. The RM sees a [WaitingPanel] — the masked destination, an
/// explanation to read aloud, and a resend — and nothing they can type into.
library;

import 'package:flutter/material.dart';

import '../app.dart';
import '../design/components.dart';
import '../design/tokens.dart';
import '../domain/consent.dart';
import '../guards/journey_guard.dart';
import 'customer_and_lead.dart';

class ConsentScreen extends StatefulWidget {
  const ConsentScreen({super.key});

  @override
  State<ConsentScreen> createState() => _ConsentScreenState();
}

class _ConsentScreenState extends State<ConsentScreen> {
  bool _dispatched = false;

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final granted = c.consentRef(ConsentCode.solicitation) != null;

    return AppScreen(
      title: 'Customer consent',
      screenId: 'SCR-06',
      steps: kJourneySteps,
      currentStep: 1,
      footer: granted
          ? AppButton(
              label: 'Continue',
              icon: Icons.arrow_forward,
              onPressed: () => go(context, AppRoute.prefillReview),
            )
          : AppButton(
              label: _dispatched
                  ? 'Waiting for the customer'
                  : 'Send consent request to the customer',
              icon: Icons.send,
              loading: c.busy,
              onPressed: _dispatched
                  ? null
                  : () async {
                      await c.requestConsents();
                      if (mounted) setState(() => _dispatched = true);
                    },
            ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading(
            'Consent',
            subtitle:
                'The customer confirms on their own device. Read the statements '
                'aloud, then send the request.',
          ),
          for (final code in const [
            ConsentCode.dataProcessing,
            ConsentCode.solicitation,
            ConsentCode.communications,
          ])
            Padding(
              padding: const EdgeInsets.only(bottom: AppSpace.x3),
              child: AppCard(
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(code.shortLabel, style: AppType.bodyStrong),
                          const SizedBox(height: AppSpace.x1),
                          Text(code.code,
                              style: AppType.caption
                                  .copyWith(color: AppColor.textSecondary)),
                        ],
                      ),
                    ),
                    StatusChip(
                      label: c.consentRef(code) != null ? 'Granted' : 'Pending',
                      tone: c.consentRef(code) != null
                          ? StatusTone.success
                          : StatusTone.pending,
                    ),
                  ],
                ),
              ),
            ),
          const SizedBox(height: AppSpace.x4),
          Container(
            padding: const EdgeInsets.all(AppSpace.x4),
            decoration: BoxDecoration(
              color: AppColor.statusWarning.withOpacity(0.06),
              borderRadius: AppRadius.mdAll,
              border:
                  Border.all(color: AppColor.statusWarning.withOpacity(0.4)),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.gavel_outlined,
                    size: 20, color: AppColor.statusWarning),
                const SizedBox(width: AppSpace.x3),
                Expanded(
                  child: Text(
                    'Consent statement wording is placeholder text pending '
                    'Compliance approval (S05-OPEN-03). It must be replaced '
                    'before any pilot.',
                    style: AppType.body.copyWith(color: AppColor.statusWarning),
                  ),
                ),
              ],
            ),
          ),
          if (_dispatched && !granted) ...[
            const SizedBox(height: AppSpace.x5),
            WaitingPanel(
              title: 'Waiting for the customer to confirm',
              maskedDestination: c.customer!.maskedMobile.lastTwo,
              explanation:
                  'The customer will see the consent statements and a one-time '
                  'code on their own phone. You cannot enter that code here — '
                  'the customer confirms it themselves.',
              onResend: () async => c.requestConsents(),
              // Present only so the journey can be walked without a real SMS
              // gateway. It stands in for the platform reporting verification,
              // not for the RM performing it.
              extra: AppButton(
                label: 'Simulate: customer confirmed on their phone',
                variant: AppButtonVariant.secondary,
                icon: Icons.smartphone,
                onPressed: () async {
                  await c.customerVerifiedConsents();
                  if (mounted) setState(() {});
                },
              ),
            ),
          ],
          if (granted) ...[
            const SizedBox(height: AppSpace.x5),
            AppCard(
              child: Row(
                children: [
                  const Icon(Icons.verified_outlined,
                      color: AppColor.statusSuccess),
                  const SizedBox(width: AppSpace.x3),
                  Expanded(
                    child: Text(
                      'Consent captured on the customer\'s device, with a '
                      'verified one-time code. Evidence is stored append-only.',
                      style: AppType.body,
                    ),
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
