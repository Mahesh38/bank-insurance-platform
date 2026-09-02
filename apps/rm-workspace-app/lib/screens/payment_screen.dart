/// SCR-16 — Payment hand-off, RM view.
///
/// ## What is deliberately absent from this file
///
/// There is no `TextField`, no `TextFormField`, no card number, no CVV, no
/// expiry, no account number and no UPI handle — anywhere in this screen or in
/// anything it can reach. That absence is the control, not a design choice:
///
/// * RBI cyber-security guidance prohibits accepting insurance premium payment
///   on a bank employee's device.
/// * INV-PAY-01 (control C4) states no API path issues a payment link into an
///   RM session and no RM principal may complete authorisation.
/// * `AC-PAY-010-2` requires that the RM surface render no payment form and no
///   card or UPI input at any point.
///
/// `PaymentRepository` offers no method that accepts an instrument, so even a
/// future developer who wanted to add a field here would have nothing to call.
/// `test/guards/payment_device_isolation_test.dart` asserts both halves: zero
/// text inputs on this screen, and zero payment-instrument identifiers anywhere
/// under `lib/`.
library;

import 'package:flutter/material.dart';

import '../app.dart';
import '../design/components.dart';
import '../design/tokens.dart';
import '../domain/sales.dart';
import '../guards/journey_guard.dart';
import 'customer_and_lead.dart';

class PaymentHandoffScreen extends StatelessWidget {
  const PaymentHandoffScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = JourneyScope.of(context);
    final handoff = c.paymentHandoff;
    final payment = c.payment;
    final reconciled = payment?.state == PaymentState.reconciled;

    return AppScreen(
      title: 'Payment',
      screenId: 'SCR-16',
      steps: kJourneySteps,
      currentStep: 6,
      footer: reconciled
          ? AppButton(
              label: 'Continue to issuance',
              icon: Icons.arrow_forward,
              loading: c.busy,
              onPressed: () async {
                await c.issuePolicy();
                if (context.mounted && c.lastError == null) {
                  go(context, AppRoute.confirmation);
                }
              },
            )
          : handoff == null
              ? AppButton(
                  label: 'Send payment link to the customer',
                  icon: Icons.send,
                  loading: c.busy,
                  onPressed: () async => c.issuePaymentLink(),
                )
              : null,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SectionHeading(
            'Payment on the customer\'s device',
            subtitle:
                'The customer pays on their own phone, through the AU Bank '
                'payment gateway. Payment is never taken on this device.',
          ),
          Container(
            padding: const EdgeInsets.all(AppSpace.x4),
            decoration: BoxDecoration(
              color: AppColor.statusInfo.withOpacity(0.05),
              borderRadius: AppRadius.mdAll,
              border: Border.all(color: AppColor.statusInfo.withOpacity(0.3)),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.phonelink_lock_outlined,
                    color: AppColor.statusInfo),
                const SizedBox(width: AppSpace.x3),
                Expanded(
                  child: Text(
                    'Do not take the customer\'s phone and do not collect card '
                    'or account details. This screen shows status only.',
                    style: AppType.bodyStrong
                        .copyWith(color: AppColor.statusInfo),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: AppSpace.x5),
          if (handoff == null)
            const EmptyState(
              title: 'No payment link issued yet.',
              explanation:
                  'Sending the link delivers it to the customer\'s registered '
                  'mobile. They complete payment on their own device.',
            )
          else ...[
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  LabelledValue('Amount',
                      '₹${handoff.amount.toStringAsFixed(0)}'),
                  LabelledValue('Sent to', handoff.sentTo.toString()),
                  LabelledValue('Link expires',
                      handoff.expiresAt.toIso8601String().substring(11, 16)),
                ],
              ),
            ),
            const SizedBox(height: AppSpace.x4),
            _HandoffCode(url: handoff.customerDeviceUrl),
            const SizedBox(height: AppSpace.x4),
            if (!reconciled)
              WaitingPanel(
                title: 'Waiting for the customer to pay',
                maskedDestination: handoff.sentTo.lastTwo,
                explanation:
                    'The customer opens the link on their own phone and pays '
                    'through the AU Bank gateway. You will see the status change '
                    'here. There is nothing for you to enter.',
                onResend: () async => c.issuePaymentLink(),
                extra: AppButton(
                  label: 'Simulate: customer paid on their own device',
                  variant: AppButtonVariant.secondary,
                  icon: Icons.smartphone,
                  onPressed: () async => c.customerCompletesPayment(),
                ),
              )
            else
              AppCard(
                child: Row(
                  children: [
                    const Icon(Icons.check_circle_outline,
                        color: AppColor.statusSuccess),
                    const SizedBox(width: AppSpace.x3),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('Payment received and reconciled',
                              style: AppType.bodyStrong),
                          const SizedBox(height: AppSpace.x1),
                          Text(
                            'Reconciled against the gateway settlement, which '
                            'is what permits issuance. Captured alone is not '
                            'enough.',
                            style: AppType.caption
                                .copyWith(color: AppColor.textSecondary),
                          ),
                        ],
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

/// The hand-off panel.
///
/// A real scannable QR encoder is a later increment. Rendering a decorative
/// block that *looks* like a QR would be worse than showing none — an RM would
/// try to scan it — so this states plainly what it is and shows the link the
/// customer receives.
class _HandoffCode extends StatelessWidget {
  final String url;
  const _HandoffCode({required this.url});

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.qr_code_2, color: AppColor.textSecondary),
              SizedBox(width: AppSpace.x2),
              Text('Customer payment link', style: AppType.headingS),
            ],
          ),
          const SizedBox(height: AppSpace.x3),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(AppSpace.x4),
            decoration: BoxDecoration(
              color: AppColor.surfaceMuted,
              borderRadius: AppRadius.smAll,
              border: Border.all(color: AppColor.border),
            ),
            child: SelectableText(url, style: AppType.mono),
          ),
          const SizedBox(height: AppSpace.x3),
          Text(
            'Scannable QR rendering is a later increment. The link is delivered '
            'to the customer by SMS; it is never opened on this device and is '
            'never written to a log.',
            style: AppType.caption.copyWith(color: AppColor.textSecondary),
          ),
        ],
      ),
    );
  }
}
