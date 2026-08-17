/// The screen a refused route renders instead of its destination.
///
/// It names the rule that refused, in RM-facing language, and offers the one
/// action that recovers. `S05-experience-evidence.md` §4.5 requires every error
/// state to carry actionable copy — a refusal that says only "not allowed"
/// teaches the RM that the system is arbitrary, which is how workarounds start.
library;

import 'package:flutter/material.dart';

import '../app.dart';
import '../design/components.dart';
import '../design/tokens.dart';
import '../guards/journey_guard.dart';

class GuardBlockedScreen extends StatelessWidget {
  final AppRoute attempted;
  final GuardRefusal refusal;

  const GuardBlockedScreen({
    super.key,
    required this.attempted,
    required this.refusal,
  });

  @override
  Widget build(BuildContext context) {
    return AppScreen(
      title: 'Step not available yet',
      screenId: 'GUARD',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          EmptyState(
            blocked: true,
            title: refusal.rmMessage,
            explanation:
                'This step cannot be opened until the requirement above is met. '
                'It is a platform control, not a preference, and it cannot be '
                'skipped or overridden from this screen.',
            action: AppButton(
              label: 'Go to the step that unblocks this',
              icon: Icons.arrow_forward,
              onPressed: () => goReplace(context, refusal.recoverAt),
            ),
          ),
          const SizedBox(height: AppSpace.x7),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.shield_outlined,
                        size: 18, color: AppColor.textSecondary),
                    const SizedBox(width: AppSpace.x2),
                    Text('Why this was refused',
                        style: AppType.label
                            .copyWith(color: AppColor.textSecondary)),
                  ],
                ),
                const SizedBox(height: AppSpace.x3),
                LabelledValue('Attempted step', attempted.path, mono: true),
                LabelledValue('Refusal code', refusal.code, mono: true),
                LabelledValue('Rule', refusal.rule),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
