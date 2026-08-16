/// Design tokens — AU Bank Insurance Distribution Platform, RM workspace.
///
/// These are a direct implementation of the design-system specification in
/// `docs/application-lifecycle-bible/evidence/S05-experience-evidence.md` §4.6.
/// Values are not invented here; if a token changes it changes in that spec
/// first and this file follows.
///
/// BRAND SUBSTITUTION (S05-OPEN-04): the primary ramp is a placeholder meeting
/// WCAG 2.1 AA against the stated backgrounds. AU Bank's real brand values must
/// be substituted and every contrast ratio re-verified before pilot.
library;

import 'package:flutter/material.dart';

/// Colour tokens. Light values only — R0 ships a single light theme.
abstract final class AppColor {
  // Brand
  static const brandPrimary = Color(0xFF0B4F9E); // 7.4:1 on surface
  static const brandPrimaryHover = Color(0xFF093F7E);
  static const onPrimary = Color(0xFFFFFFFF);

  // Surfaces
  static const surface = Color(0xFFFFFFFF);
  static const surfaceMuted = Color(0xFFF4F6F8);
  static const surfaceRaised = Color(0xFFFFFFFF);

  // Borders
  static const border = Color(0xFFD5DBE1);
  static const borderStrong = Color(0xFF9AA5B1);

  // Text
  static const textPrimary = Color(0xFF111820); // 16.1:1
  static const textSecondary = Color(0xFF4A555F); // 7.6:1
  static const textDisabled = Color(0xFF8A949E); // never used for meaning

  // Status — DS-01: never the sole carrier of meaning
  static const statusSuccess = Color(0xFF1B7F4B);
  static const statusWarning = Color(0xFF8A5A00);
  static const statusError = Color(0xFFB3261E);
  static const statusInfo = Color(0xFF0B4F9E);
  static const statusPending = Color(0xFF4A555F);

  static const focusRing = Color(0xFF0B4F9E);
}

/// Type scale — 1.200 minor third, 16px base.
///
/// DS-02: regulated copy renders at [body] or larger, never at [caption].
abstract final class AppType {
  static const display =
      TextStyle(fontSize: 33, height: 40 / 33, fontWeight: FontWeight.w600);
  static const headingL =
      TextStyle(fontSize: 28, height: 36 / 28, fontWeight: FontWeight.w600);
  static const headingM =
      TextStyle(fontSize: 23, height: 32 / 23, fontWeight: FontWeight.w600);
  static const headingS =
      TextStyle(fontSize: 19, height: 28 / 19, fontWeight: FontWeight.w600);
  static const body =
      TextStyle(fontSize: 16, height: 24 / 16, fontWeight: FontWeight.w400);
  static const bodyStrong =
      TextStyle(fontSize: 16, height: 24 / 16, fontWeight: FontWeight.w600);
  static const label =
      TextStyle(fontSize: 14, height: 20 / 14, fontWeight: FontWeight.w500);
  static const caption =
      TextStyle(fontSize: 13, height: 18 / 13, fontWeight: FontWeight.w400);
  static const mono = TextStyle(
      fontSize: 15,
      height: 22 / 15,
      fontWeight: FontWeight.w400,
      fontFamily: 'monospace');
}

/// Spacing — 4px base.
abstract final class AppSpace {
  static const none = 0.0;
  static const x1 = 4.0;
  static const x2 = 8.0;
  static const x3 = 12.0;
  static const x4 = 16.0;
  static const x5 = 24.0;
  static const x6 = 32.0;
  static const x7 = 48.0;
  static const x8 = 64.0;
}

/// Radius tokens.
abstract final class AppRadius {
  static const sm = Radius.circular(4);
  static const md = Radius.circular(8);
  static const lg = Radius.circular(16);
  static const pill = Radius.circular(999);

  static const smAll = BorderRadius.all(sm);
  static const mdAll = BorderRadius.all(md);
  static const lgAll = BorderRadius.all(lg);
  static const pillAll = BorderRadius.all(pill);
}

/// Elevation tokens.
abstract final class AppElevation {
  static const List<BoxShadow> level0 = <BoxShadow>[];
  static const List<BoxShadow> level1 = [
    BoxShadow(color: Color(0x14000000), blurRadius: 2, offset: Offset(0, 1)),
  ];
  static const List<BoxShadow> level2 = [
    BoxShadow(color: Color(0x1F000000), blurRadius: 12, offset: Offset(0, 4)),
  ];
}

/// Motion tokens.
///
/// DS-03: motion is decorative and must be removable. No journey step may
/// depend on an animation completing.
abstract final class AppMotion {
  static const fast = Duration(milliseconds: 120);
  static const base = Duration(milliseconds: 200);
  static const slow = Duration(milliseconds: 320);
  static const curve = Cubic(0.2, 0, 0, 1);
}

/// Responsive breakpoints. `medium` — the RM tablet in branch — is the
/// primary R0 case.
abstract final class AppBreakpoint {
  static const compact = 600.0;
  static const expanded = 1024.0;

  static bool isCompact(double width) => width < compact;
  static bool isMedium(double width) => width >= compact && width < expanded;
  static bool isExpanded(double width) => width >= expanded;
}

/// Minimum touch target, per the accessibility standard in S05 §4.7.
const double kMinTouchTarget = 48.0;

ThemeData buildAppTheme() {
  final base = ThemeData.light(useMaterial3: true);
  return base.copyWith(
    scaffoldBackgroundColor: AppColor.surfaceMuted,
    colorScheme: base.colorScheme.copyWith(
      primary: AppColor.brandPrimary,
      onPrimary: AppColor.onPrimary,
      surface: AppColor.surface,
      error: AppColor.statusError,
    ),
    textTheme: base.textTheme.copyWith(
      displayLarge: AppType.display,
      headlineLarge: AppType.headingL,
      headlineMedium: AppType.headingM,
      headlineSmall: AppType.headingS,
      bodyLarge: AppType.body,
      labelLarge: AppType.label,
      bodySmall: AppType.caption,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: AppColor.brandPrimary,
      foregroundColor: AppColor.onPrimary,
      elevation: 0,
    ),
  );
}
