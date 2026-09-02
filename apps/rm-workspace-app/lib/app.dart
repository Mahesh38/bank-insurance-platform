/// Application shell and the guarded router.
///
/// Every navigation goes through [AppRouter.generate], which consults
/// [JourneyGuard] BEFORE constructing a screen. A refused route builds
/// [GuardBlockedScreen] instead — the target screen is never instantiated, so
/// there is no rendered form with a disabled button to work around.
library;

import 'package:flutter/material.dart';

import 'design/tokens.dart';
import 'guards/journey_guard.dart';
import 'screens/auth_and_pipeline.dart';
import 'screens/confirmation_screen.dart';
import 'screens/consent_screen.dart';
import 'screens/customer_and_lead.dart';
import 'screens/guard_blocked.dart';
import 'screens/payment_screen.dart';
import 'screens/proposal_screens.dart';
import 'screens/quote_screens.dart';
import 'screens/suitability_screens.dart';
import 'state/journey_controller.dart';

/// Exposes the controller to the widget tree without a third-party package.
class JourneyScope extends InheritedNotifier<JourneyController> {
  const JourneyScope({
    super.key,
    required JourneyController controller,
    required super.child,
  }) : super(notifier: controller);

  static JourneyController of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<JourneyScope>();
    assert(scope != null, 'JourneyScope is missing from the widget tree');
    return scope!.notifier!;
  }
}

class RmWorkspaceApp extends StatefulWidget {
  final JourneyController? controller;
  final AppRoute initialRoute;

  const RmWorkspaceApp({
    super.key,
    this.controller,
    this.initialRoute = AppRoute.login,
  });

  @override
  State<RmWorkspaceApp> createState() => _RmWorkspaceAppState();
}

class _RmWorkspaceAppState extends State<RmWorkspaceApp> {
  late final JourneyController _controller =
      widget.controller ?? JourneyController.withFake();

  @override
  void dispose() {
    if (widget.controller == null) _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return JourneyScope(
      controller: _controller,
      child: MaterialApp(
        title: 'AU Bank — RM Workspace (R0)',
        debugShowCheckedModeBanner: false,
        theme: buildAppTheme(),
        initialRoute: widget.initialRoute.path,
        onGenerateRoute: (settings) =>
            AppRouter.generate(settings, _controller),
      ),
    );
  }
}

abstract final class AppRouter {
  /// The single navigation entry point.
  ///
  /// The guard runs first. Only if it returns `null` is the destination screen
  /// constructed.
  static Route<dynamic> generate(
      RouteSettings settings, JourneyController controller) {
    final route = AppRoute.fromPath(settings.name ?? AppRoute.login.path) ??
        AppRoute.login;

    final refusal = JourneyGuard.check(route, controller);
    if (refusal != null) {
      return MaterialPageRoute(
        settings: settings,
        builder: (_) => GuardBlockedScreen(
          attempted: route,
          refusal: refusal,
        ),
      );
    }

    return MaterialPageRoute(
      settings: settings,
      builder: (_) => _screenFor(route),
    );
  }

  static Widget _screenFor(AppRoute route) => switch (route) {
        AppRoute.login => const LoginScreen(),
        AppRoute.pipeline => const PipelineScreen(),
        AppRoute.funnel => const FunnelScreen(),
        AppRoute.customerSearch => const CustomerSearchScreen(),
        AppRoute.customerDetail => const CustomerDetailScreen(),
        AppRoute.leadCreate => const LeadCreateScreen(),
        AppRoute.consent => const ConsentScreen(),
        AppRoute.prefillReview => const PrefillReviewScreen(),
        AppRoute.suitability => const SuitabilityQuestionnaireScreen(),
        AppRoute.suitabilityOutcome => const SuitabilityOutcomeScreen(),
        AppRoute.products => const EligibleProductsScreen(),
        AppRoute.quote => const QuoteScreen(),
        AppRoute.compare => const CompareScreen(),
        AppRoute.proposal => const ProposalFormScreen(),
        AppRoute.submit => const ProposalSubmitScreen(),
        AppRoute.status => const ApplicationStatusScreen(),
        AppRoute.payment => const PaymentHandoffScreen(),
        AppRoute.confirmation => const PolicyConfirmationScreen(),
      };
}

/// Navigates, letting the guard decide what actually renders.
void go(BuildContext context, AppRoute route) {
  Navigator.of(context).pushNamed(route.path);
}

void goReplace(BuildContext context, AppRoute route) {
  Navigator.of(context).pushReplacementNamed(route.path);
}
