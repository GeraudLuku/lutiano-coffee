import 'package:intl/intl.dart';
import 'package:lutiano_seller/core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

/// A MaterialApp for widget tests: English strings and the app's translation delegates, so `context.l10n` works.
Widget testApp({required Widget home}) {
  Intl.defaultLocale = 'en';
  return MaterialApp(
    theme: AppTheme.light(),
    scrollBehavior: const AppScrollBehavior(),
    locale: const Locale('en'),
    localizationsDelegates: [
      AppLocalizations.delegate,
      CoreLocalizations.delegate,
      ...GlobalMaterialLocalizations.delegates
    ],
    supportedLocales: AppLocalizations.supportedLocales,
    home: home,
  );
}
