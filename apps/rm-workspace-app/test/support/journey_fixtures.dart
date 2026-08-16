/// Test fixtures — drive a controller to a known point in the R0 journey.
///
/// Deliberately built on the real `FakeBackend` rather than on mocks, so the
/// guard tests exercise the same state machines the application uses. A guard
/// test that passes against a mock proves the mock is well behaved.
library;

import 'package:rm_workspace_app/domain/suitability.dart';
import 'package:rm_workspace_app/state/journey_controller.dart';

/// A profile that produces `SUITABLE` for Term.
///
/// This is the reference case `SUIT-TC-REF-01` from suitability-rule-pack §3.4:
/// age 38, income 12,00,000 (band B4, multiple 18, age factor 1.00),
/// 2 dependants, existing cover 5,00,000, liabilities 30,00,000,
/// declared premium 25,000.
const suitableAnswers = SuitabilityAnswers(
  ageNextBirthday: 38,
  annualIncome: 1200000,
  dependants: 2,
  declaredExistingCover: 500000,
  declaredLiabilities: 3000000,
  objective: FinancialObjective.protection,
  horizon: InvestmentHorizon.y10to20,
  riskAttitude: RiskAttitude.moderate,
  declaredAnnualPremium: 25000,
  tobaccoUse: false,
  occupation: OccupationCategory.salaried,
  priorDecline: PriorDeclineAnswer.no,
  isReplacement: false,
);

/// A profile that produces `NOT_SUITABLE` for Term via SUIT-R12 — the declared
/// premium is more than 30% of income.
const unaffordableAnswers = SuitabilityAnswers(
  ageNextBirthday: 38,
  annualIncome: 300000,
  dependants: 1,
  declaredExistingCover: 0,
  declaredLiabilities: 0,
  objective: FinancialObjective.protection,
  horizon: InvestmentHorizon.y10to20,
  riskAttitude: RiskAttitude.moderate,
  declaredAnnualPremium: 150000, // 50% of income
  tobaccoUse: false,
  occupation: OccupationCategory.salaried,
  priorDecline: PriorDeclineAnswer.no,
  isReplacement: false,
);

/// Signed in, nothing else.
Future<JourneyController> signedIn({DateTime Function()? clock}) async {
  final c = JourneyController.withFake(clock: clock);
  c.signIn();
  return c;
}

/// Signed in, customer selected, lead created. Consents NOT captured.
Future<JourneyController> withLead({DateTime Function()? clock}) async {
  final c = await signedIn(clock: clock);
  final results = await c.searchCustomers('AU100');
  await c.selectCustomer(results.first);
  await c.createLead();
  return c;
}

/// Lead plus granted consents. Suitability NOT completed.
Future<JourneyController> withConsent({DateTime Function()? clock}) async {
  final c = await withLead(clock: clock);
  await c.requestConsents();
  await c.customerVerifiedConsents();
  await c.startSuitability();
  return c;
}

/// Consents plus a COMPLETED suitability assessment with the given answers.
Future<JourneyController> withSuitability({
  SuitabilityAnswers answers = suitableAnswers,
  DateTime Function()? clock,
}) async {
  final c = await withConsent(clock: clock);
  await c.completeSuitability(answers);
  return c;
}

/// Suitability plus a requested quote and a selected offer.
Future<JourneyController> withSelectedOffer({DateTime Function()? clock}) async {
  final c = await withSuitability(clock: clock);
  await c.loadEligibleProducts();
  await c.requestQuote(
    product: c.eligibleProducts.first,
    sumAssured: 5000000,
    termYears: 25,
  );
  await c.selectOffer(c.quote!.rankedOffers.first.id);
  return c;
}

/// Selected offer plus the insurer-sharing consent — the point at which the
/// proposal step becomes reachable.
Future<JourneyController> withSharingConsent({DateTime Function()? clock}) async {
  final c = await withSelectedOffer(clock: clock);
  await c.captureSharingConsent();
  return c;
}
