import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:intl/intl.dart';
import 'package:lutiano_seller/core/l10n.dart';
import 'package:lutiano_seller/features/auth/forgot_password_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

import 'support/fake_api.dart';

Map<String, dynamic> _arb(String locale) =>
    jsonDecode(File('lib/l10n/app_$locale.arb').readAsStringSync())
        as Map<String, dynamic>;

Set<String> _keys(Map<String, dynamic> arb) =>
    arb.keys.where((k) => !k.startsWith('@')).toSet();

/// {name} placeholders used in an ICU message (plural bodies are skipped by taking only simple names).
Set<String> _placeholders(String message) => RegExp(r'\{(\w+)(?:,|\})')
    .allMatches(message)
    .map((m) => m.group(1)!)
    .toSet();

void main() {
  final en = _arb('en'), fr = _arb('fr');

  test('French has every string English has, and nothing else', () {
    expect(_keys(fr).difference(_keys(en)), isEmpty,
        reason: 'French strings without an English original');
    expect(_keys(en).difference(_keys(fr)), isEmpty,
        reason: 'English strings still to translate');
  });

  test('every French string uses the same placeholders as its English original',
      () {
    for (final k in _keys(en).where((k) => k != '@@locale')) {
      expect(_placeholders(fr[k] as String), _placeholders(en[k] as String),
          reason: k);
    }
  });

  test('no string is empty', () {
    for (final arb in [en, fr]) {
      for (final k in _keys(arb).where((k) => k != '@@locale')) {
        expect((arb[k] as String).trim(), isNotEmpty, reason: k);
      }
    }
  });

  testWidgets('screens show French when the app language is French',
      (tester) async {
    Intl.defaultLocale = 'fr';
    addTearDown(() => Intl.defaultLocale = 'en');
    await tester.pumpWidget(AppScope(
      state: AppState(api: FakeApi({}).client()),
      child: MaterialApp(
        locale: const Locale('fr'),
        localizationsDelegates: [
          AppLocalizations.delegate,
          ...GlobalMaterialLocalizations.delegates
        ],
        supportedLocales: AppLocalizations.supportedLocales,
        home: const ForgotPasswordScreen(),
      ),
    ));
    expect(find.text('Mot de passe oublié'), findsOneWidget);
    expect(find.text('Envoyer le code'), findsOneWidget);
  });
}
