/// The guards, asserted as *structure* rather than as behaviour.
///
/// The behavioural tests in this directory prove the guards work. These tests
/// prove they cannot be casually removed — that the property holding them up is
/// the type system and the interface shape, not a validation call someone can
/// delete.
///
/// A behavioural test says "this input is refused". A structural test says
/// "there is no way to express the input". The second is what makes a control
/// survive a refactor by someone who has not read the rule pack.
library;

import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import 'payment_device_isolation_test.dart' show stripDartComments;

String _source(String path) => stripDartComments(File(path).readAsStringSync());

void main() {
  group('capability tokens have library-private constructors', () {
    test('SuitabilityEvaluationRef can only be minted inside its own library',
        () {
      final src = _source('lib/domain/suitability.dart');

      // The constructor is private...
      expect(src.contains('const SuitabilityEvaluationRef._('), isTrue,
          reason: 'the token constructor must be library-private');

      // ...and there is exactly one call site, inside evaluationRef.
      final mintSites = 'SuitabilityEvaluationRef._('.allMatches(src).length;
      expect(mintSites, 2,
          reason: 'exactly one declaration and one mint site expected; '
              'found $mintSites occurrences');

      expect(src.contains('SuitabilityEvaluationRef? evaluationRef('), isTrue);
    });

    test('ConsentGrantRef can only be minted inside its own library', () {
      final src = _source('lib/domain/consent.dart');

      expect(src.contains('const ConsentGrantRef._('), isTrue);
      final mintSites = 'ConsentGrantRef._('.allMatches(src).length;
      expect(mintSites, 2,
          reason: 'exactly one declaration and one mint site expected; '
              'found $mintSites occurrences');

      expect(src.contains('ConsentGrantRef? grantRef('), isTrue);
    });

    test('no other library constructs a token directly', () {
      final offenders = <String>[];

      for (final entity in Directory('lib').listSync(recursive: true)) {
        if (entity is! File || !entity.path.endsWith('.dart')) continue;
        if (entity.path.endsWith('domain/suitability.dart')) continue;
        if (entity.path.endsWith('domain/consent.dart')) continue;

        final src = stripDartComments(entity.readAsStringSync());
        if (src.contains('SuitabilityEvaluationRef._(') ||
            src.contains('ConsentGrantRef._(')) {
          offenders.add(entity.path);
        }
      }

      expect(offenders, isEmpty,
          reason: 'capability tokens must be minted only by their own '
              'aggregate. Offenders: $offenders');
    });
  });

  group('the repository signatures carry the guards', () {
    late String repositories;

    setUp(() => repositories = _source('lib/data/repositories.dart'));

    test('requestQuote requires a SuitabilityEvaluationRef — SUIT-R20', () {
      expect(
        repositories.contains('required SuitabilityEvaluationRef suitabilityRef'),
        isTrue,
        reason: 'the quote path must take the capability token, so that a '
            'caller without a valid evaluation has nothing to pass',
      );
    });

    test('eligibleProducts also requires the token — CAT-R02', () {
      final catalogueBlock = repositories.substring(
        repositories.indexOf('abstract interface class CatalogueRepository'),
        repositories.indexOf('abstract interface class QuoteRepository'),
      );
      expect(catalogueBlock.contains('SuitabilityEvaluationRef'), isTrue);
    });

    test('proposal submit requires a ConsentGrantRef — INV-PRP-01', () {
      expect(
        repositories.contains('required ConsentGrantRef sharingConsent'),
        isTrue,
      );
    });

    test('customer prefill requires a data-processing grant — AC-CUST-020-2',
        () {
      expect(
        repositories.contains('required ConsentGrantRef dataProcessingConsent'),
        isTrue,
      );
    });

    test('suitability start requires a solicitation grant — SUIT-R21', () {
      expect(
        repositories.contains('required ConsentGrantRef solicitationConsent'),
        isTrue,
      );
    });
  });

  group('the router consults the guard before building any screen', () {
    test('AppRouter.generate checks JourneyGuard first', () {
      final src = _source('lib/app.dart');

      final guardIndex = src.indexOf('JourneyGuard.check(route, controller)');
      final screenIndex = src.indexOf('_screenFor(route)');

      expect(guardIndex, greaterThan(-1),
          reason: 'the router must consult the guard');
      expect(screenIndex, greaterThan(-1));
      expect(guardIndex, lessThan(screenIndex),
          reason: 'the guard must run BEFORE the destination screen is built');
    });

    test('every route in AppRoute is handled by the guard switch', () {
      final guard = _source('lib/guards/journey_guard.dart');

      // Anchored on declarations, not on comment text — comments are stripped
      // before scanning, so a comment marker would not be there to find.
      final enumBlock = guard.substring(
        guard.indexOf('enum AppRoute'),
        guard.indexOf('class GuardRefusal'),
      );

      // Enum values are declared as `name('/path'),`.
      final names = RegExp(r"^\s*(\w+)\('/", multiLine: true)
          .allMatches(enumBlock)
          .map((m) => m.group(1)!)
          .toList();

      expect(names.length, greaterThanOrEqualTo(18),
          reason: 'expected every R0 route to be declared; found $names');

      final switchBody = guard.substring(guard.indexOf('switch (route)'));
      for (final name in names) {
        expect(switchBody.contains('AppRoute.$name'), isTrue,
            reason: 'route "$name" is not handled by JourneyGuard.check — an '
                'unguarded route is an ungated route');
      }
    });
  });

  group('the application has no third-party runtime dependencies', () {
    test('pubspec declares no package beyond the Flutter defaults', () {
      final pubspec = File('pubspec.yaml').readAsStringSync();

      // Strip YAML comments, then take everything from `dependencies:` to the
      // final `flutter:` section.
      final body = pubspec
          .split('\n')
          .where((l) => !l.trimLeft().startsWith('#'))
          .join('\n');

      final deps = body.substring(body.indexOf('dependencies:'));

      // Two-space-indented keys are package names inside a dependency block.
      final packages = RegExp(r'^\s{2}(\w+):', multiLine: true)
          .allMatches(deps)
          .map((m) => m.group(1)!)
          .toSet();

      // `cupertino_icons` and `flutter_lints` ship with `flutter create`.
      // Anything beyond these is a supply-chain surface the S08 SCA scan would
      // have to cover, and R0 deliberately has none.
      const allowed = {
        'flutter',
        'flutter_test',
        'cupertino_icons',
        'flutter_lints',
        'sdk',
        'uses',
      };

      expect(
        packages.difference(allowed),
        isEmpty,
        reason: 'unexpected third-party dependency in pubspec: '
            '${packages.difference(allowed)}',
      );
    });
  });
}
