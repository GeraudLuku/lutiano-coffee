import 'package:intl/intl.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:lutiano_customer/core/l10n.dart';
import 'package:lutiano_customer/state/app_scope.dart';
import 'package:lutiano_customer/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

/// A MaterialApp for widget tests: English strings and the app's translation delegates, with [state] in scope.
Widget testApp({required Widget home, required AppState state}) {
  Intl.defaultLocale = 'en';
  return AppScope(
    state: state,
    child: MediaUrls(
      toUrl: state.api.mediaUrl,
      child: MaterialApp(
        theme: AppTheme.light(),
        scrollBehavior: const AppScrollBehavior(),
        locale: const Locale('en'),
        localizationsDelegates: const [
          AppLocalizations.delegate,
          CoreLocalizations.delegate,
          ...GlobalMaterialLocalizations.delegates
        ],
        supportedLocales: AppLocalizations.supportedLocales,
        home: home,
      ),
    ),
  );
}
