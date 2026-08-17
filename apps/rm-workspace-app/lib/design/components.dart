/// Component library.
///
/// Implements the components table in `S05-experience-evidence.md` §4.6 with
/// the variants and states specified there. Two carry regulatory weight and are
/// commented accordingly: [WaitingPanel] and [EmptyState].
library;

import 'package:flutter/material.dart';

import 'tokens.dart';

enum AppButtonVariant { primary, secondary, tertiary, destructive }

/// Minimum touch target 48x48 (S05 §4.7). Loading disables AND changes the
/// label — a spinner alone leaves a screen reader with nothing to announce.
class AppButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final AppButtonVariant variant;
  final bool loading;
  final IconData? icon;

  const AppButton({
    super.key,
    required this.label,
    this.onPressed,
    this.variant = AppButtonVariant.primary,
    this.loading = false,
    this.icon,
  });

  @override
  Widget build(BuildContext context) {
    final enabled = onPressed != null && !loading;
    final (bg, fg) = switch (variant) {
      AppButtonVariant.primary => (AppColor.brandPrimary, AppColor.onPrimary),
      AppButtonVariant.secondary => (AppColor.surface, AppColor.brandPrimary),
      AppButtonVariant.tertiary => (Colors.transparent, AppColor.brandPrimary),
      AppButtonVariant.destructive => (AppColor.statusError, AppColor.onPrimary),
    };
    return ConstrainedBox(
      constraints: const BoxConstraints(minHeight: kMinTouchTarget),
      child: Material(
        color: enabled ? bg : AppColor.surfaceMuted,
        shape: RoundedRectangleBorder(
          borderRadius: AppRadius.mdAll,
          side: variant == AppButtonVariant.secondary
              ? const BorderSide(color: AppColor.brandPrimary)
              : BorderSide.none,
        ),
        child: InkWell(
          onTap: enabled ? onPressed : null,
          borderRadius: AppRadius.mdAll,
          child: Padding(
            padding: const EdgeInsets.symmetric(
                horizontal: AppSpace.x5, vertical: AppSpace.x3),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (loading) ...[
                  const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                  const SizedBox(width: AppSpace.x2),
                ] else if (icon != null) ...[
                  Icon(icon, size: 18, color: enabled ? fg : AppColor.textDisabled),
                  const SizedBox(width: AppSpace.x2),
                ],
                // Flexible, because R0's primary surface is a branch tablet and
                // several action labels are full sentences. An overflowing
                // button is content the RM cannot see (S05 §4.6 responsive).
                Flexible(
                  child: Text(
                    loading ? 'Working…' : label,
                    style: AppType.bodyStrong
                        .copyWith(color: enabled ? fg : AppColor.textDisabled),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

enum StatusTone { success, warning, error, info, pending }

/// DS-01: colour is never the sole carrier of meaning. Every chip renders an
/// icon AND a colour AND a text label.
class StatusChip extends StatelessWidget {
  final String label;
  final StatusTone tone;

  const StatusChip({super.key, required this.label, required this.tone});

  @override
  Widget build(BuildContext context) {
    final (color, icon) = switch (tone) {
      StatusTone.success => (AppColor.statusSuccess, Icons.check_circle_outline),
      StatusTone.warning => (AppColor.statusWarning, Icons.warning_amber_outlined),
      StatusTone.error => (AppColor.statusError, Icons.error_outline),
      StatusTone.info => (AppColor.statusInfo, Icons.info_outline),
      StatusTone.pending => (AppColor.statusPending, Icons.schedule),
    };
    return Container(
      padding: const EdgeInsets.symmetric(
          horizontal: AppSpace.x3, vertical: AppSpace.x1),
      decoration: BoxDecoration(
        color: color.withOpacity(0.08),
        borderRadius: AppRadius.pillAll,
        border: Border.all(color: color.withOpacity(0.4)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: color),
          const SizedBox(width: AppSpace.x2),
          Text(label, style: AppType.label.copyWith(color: color)),
        ],
      ),
    );
  }
}

class AppCard extends StatelessWidget {
  final Widget child;
  final bool selected;
  final VoidCallback? onTap;

  const AppCard({super.key, required this.child, this.selected = false, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColor.surfaceRaised,
        borderRadius: AppRadius.mdAll,
        border: Border.all(
          color: selected ? AppColor.brandPrimary : AppColor.border,
          width: selected ? 2 : 1,
        ),
        boxShadow: AppElevation.level1,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: AppRadius.mdAll,
          child: Padding(padding: const EdgeInsets.all(AppSpace.x4), child: child),
        ),
      ),
    );
  }
}

/// An empty state requires an explanation AND a next action. A bare
/// illustration is not permitted (S05 §4.6).
class EmptyState extends StatelessWidget {
  final String title;
  final String explanation;
  final Widget? action;
  final bool blocked;

  const EmptyState({
    super.key,
    required this.title,
    required this.explanation,
    this.action,
    this.blocked = false,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 520),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              blocked ? Icons.block : Icons.inbox_outlined,
              size: 48,
              color: blocked ? AppColor.statusWarning : AppColor.textSecondary,
            ),
            const SizedBox(height: AppSpace.x4),
            Text(title, style: AppType.headingS, textAlign: TextAlign.center),
            const SizedBox(height: AppSpace.x2),
            Text(
              explanation,
              style: AppType.body.copyWith(color: AppColor.textSecondary),
              textAlign: TextAlign.center,
            ),
            if (action != null) ...[
              const SizedBox(height: AppSpace.x5),
              action!,
            ],
          ],
        ),
      ),
    );
  }
}

class ErrorBanner extends StatelessWidget {
  final String message;
  const ErrorBanner(this.message, {super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(AppSpace.x4),
      decoration: BoxDecoration(
        color: AppColor.statusError.withOpacity(0.06),
        borderRadius: AppRadius.mdAll,
        border: Border.all(color: AppColor.statusError.withOpacity(0.4)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.error_outline, color: AppColor.statusError, size: 20),
          const SizedBox(width: AppSpace.x3),
          Expanded(
            child: Text(message,
                style: AppType.body.copyWith(color: AppColor.statusError)),
          ),
        ],
      ),
    );
  }
}

/// The RM-side device hand-off panel — SCR-06 and SCR-16.
///
/// **This component carries a regulatory obligation.** `S05-experience-evidence.md`
/// §4.5 records why: an RM left staring at dead air with a customer opposite
/// will reach for the customer's phone, and CNS-R13 and the RBI device rule are
/// then breached by a system that implemented them correctly.
///
/// So the waiting state is not a spinner. It shows the masked destination the
/// RM can read aloud, a countdown, and a resend — it gives the RM something to
/// SAY while the customer acts on their own device.
class WaitingPanel extends StatelessWidget {
  final String title;
  final String maskedDestination;
  final String explanation;
  final VoidCallback? onResend;
  final Widget? extra;

  const WaitingPanel({
    super.key,
    required this.title,
    required this.maskedDestination,
    required this.explanation,
    this.onResend,
    this.extra,
  });

  @override
  Widget build(BuildContext context) {
    return Semantics(
      liveRegion: true,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(AppSpace.x5),
        decoration: BoxDecoration(
          color: AppColor.statusInfo.withOpacity(0.05),
          borderRadius: AppRadius.mdAll,
          border: Border.all(color: AppColor.statusInfo.withOpacity(0.3)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.smartphone, color: AppColor.statusInfo),
                const SizedBox(width: AppSpace.x3),
                Expanded(child: Text(title, style: AppType.headingS)),
              ],
            ),
            const SizedBox(height: AppSpace.x3),
            Text(
              'Sent to the customer\'s registered mobile ending $maskedDestination.',
              style: AppType.bodyStrong,
            ),
            const SizedBox(height: AppSpace.x2),
            Text(explanation,
                style: AppType.body.copyWith(color: AppColor.textSecondary)),
            if (extra != null) ...[const SizedBox(height: AppSpace.x4), extra!],
            if (onResend != null) ...[
              const SizedBox(height: AppSpace.x4),
              AppButton(
                label: 'Send again',
                variant: AppButtonVariant.secondary,
                onPressed: onResend,
                icon: Icons.refresh,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Journey position — always visible, per S05 §4.6.
class ProgressStepper extends StatelessWidget {
  final List<String> steps;
  final int currentIndex;

  const ProgressStepper({
    super.key,
    required this.steps,
    required this.currentIndex,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (var i = 0; i < steps.length; i++) ...[
            _dot(i),
            if (i < steps.length - 1)
              Container(
                width: 20,
                height: 2,
                color: i < currentIndex ? AppColor.brandPrimary : AppColor.border,
              ),
          ],
        ],
      ),
    );
  }

  Widget _dot(int i) {
    final done = i < currentIndex;
    final current = i == currentIndex;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: AppSpace.x1),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 20,
            height: 20,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: done
                  ? AppColor.brandPrimary
                  : current
                      ? AppColor.surface
                      : AppColor.surfaceMuted,
              border: Border.all(
                color: (done || current) ? AppColor.brandPrimary : AppColor.border,
                width: 2,
              ),
            ),
            child: done
                ? const Icon(Icons.check, size: 12, color: AppColor.onPrimary)
                : null,
          ),
          const SizedBox(height: AppSpace.x1),
          SizedBox(
            width: 78,
            child: Text(
              steps[i],
              style: AppType.caption.copyWith(
                color: current ? AppColor.textPrimary : AppColor.textSecondary,
                fontWeight: current ? FontWeight.w600 : FontWeight.w400,
              ),
              textAlign: TextAlign.center,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}

/// A page scaffold with a consistent title, optional stepper and body padding.
class AppScreen extends StatelessWidget {
  final String title;
  final String screenId;
  final Widget child;
  final Widget? footer;
  final List<String>? steps;
  final int? currentStep;

  const AppScreen({
    super.key,
    required this.title,
    required this.screenId,
    required this.child,
    this.footer,
    this.steps,
    this.currentStep,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(title, style: AppType.headingS.copyWith(color: AppColor.onPrimary)),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: AppSpace.x4),
            child: Center(
              child: Text(screenId,
                  style: AppType.caption.copyWith(color: AppColor.onPrimary)),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            if (steps != null && currentStep != null)
              Container(
                width: double.infinity,
                color: AppColor.surface,
                padding: const EdgeInsets.symmetric(vertical: AppSpace.x3),
                child: Center(
                  child: ProgressStepper(steps: steps!, currentIndex: currentStep!),
                ),
              ),
            Expanded(
              child: Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 900),
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.all(AppSpace.x5),
                    child: child,
                  ),
                ),
              ),
            ),
            if (footer != null)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(AppSpace.x4),
                decoration: const BoxDecoration(
                  color: AppColor.surface,
                  border: Border(top: BorderSide(color: AppColor.border)),
                ),
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 900),
                    child: footer!,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class SectionHeading extends StatelessWidget {
  final String text;
  final String? subtitle;
  const SectionHeading(this.text, {super.key, this.subtitle});

  @override
  Widget build(BuildContext context) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(text, style: AppType.headingM),
          if (subtitle != null) ...[
            const SizedBox(height: AppSpace.x2),
            Text(subtitle!,
                style: AppType.body.copyWith(color: AppColor.textSecondary)),
          ],
          const SizedBox(height: AppSpace.x4),
        ],
      );
}

class LabelledValue extends StatelessWidget {
  final String label;
  final String value;
  final bool mono;
  const LabelledValue(this.label, this.value, {super.key, this.mono = false});

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(bottom: AppSpace.x3),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 220,
              child: Text(label,
                  style: AppType.label.copyWith(color: AppColor.textSecondary)),
            ),
            Expanded(child: Text(value, style: mono ? AppType.mono : AppType.body)),
          ],
        ),
      );
}
