import 'package:intl/intl.dart';
import 'package:material_ui/material_ui.dart';

import 'core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'features/auth/login_screen.dart';
import 'features/notifications/notification_details_screen.dart';
import 'features/shell/main_shell.dart';
import 'state/app_scope.dart';
import 'state/app_state.dart';

class SellerApp extends StatefulWidget {
  const SellerApp({super.key, required this.state});
  final AppState state;

  @override
  State<SellerApp> createState() => _SellerAppState();
}

class _SellerAppState extends State<SellerApp> with WidgetsBindingObserver {
  final _navigator = GlobalKey<NavigatorState>();
  bool _wasSignedIn = false;
  Locale? _locale;
  late final PushService _push = PushService(
      register: widget.state.notifications.registerDevice,
      unregister: widget.state.notifications.unregisterDevice,
      onForeground: widget.state.refreshUnread,
      onReady: () {
        widget.state.pushRegistered = true;
        widget.state.refreshUnread();
      },
      onOpen: _openNotification);

  /// A tapped push notification opens that notification.
  void _openNotification(String id) =>
      _navigator.currentState?.push(MaterialPageRoute(
          builder: (_) => NotificationDetailsScreen(notificationId: id)));

  @override
  void initState() {
    super.initState();
    widget.state.addListener(_onStateChanged);
    widget.state.beforeSignOut = _push.stop;
    _locale = widget.state.settings.locale;
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    widget.state.removeListener(_onStateChanged);
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// Coming back to the app: look again for new notifications.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && widget.state.signedIn) {
      widget.state.refreshUnread();
    }
  }

  /// When the session ends (sign-out or expiry), drop any screens pushed on top of the shell.
  void _onStateChanged() {
    // a language chosen in Settings changes the whole app at once
    if (widget.state.settings.locale != _locale) {
      setState(() => _locale = widget.state.settings.locale);
    }
    final signedIn = widget.state.signedIn;
    if (signedIn && !_wasSignedIn) _push.start();
    if (_wasSignedIn && !signedIn) {
      _navigator.currentState?.popUntil((r) => r.isFirst);
    }
    _wasSignedIn = signedIn;
  }

  @override
  Widget build(BuildContext context) => AppScope(
        state: widget.state,
        child: MaterialApp(
          navigatorKey: _navigator,
          onGenerateTitle: (context) => context.l10n.appTitle,
          debugShowCheckedModeBanner: false,
          scrollBehavior: const AppScrollBehavior(),
          builder: (context, child) =>
              MediaUrls(toUrl: widget.state.api.mediaUrl, child: child!),
          localizationsDelegates: [
            AppLocalizations.delegate,
            CoreLocalizations.delegate,
            ...GlobalMaterialLocalizations.delegates
          ],
          supportedLocales: AppLocalizations.supportedLocales,
          // the language chosen in Settings; null follows the phone
          locale: _locale,
          // French is the default; the phone's language is used when it is French or English.
          localeResolutionCallback: (device, supported) {
            final locale = supported.firstWhere(
                (l) => l.languageCode == device?.languageCode,
                orElse: () => const Locale('fr'));
            Intl.defaultLocale =
                locale.languageCode; // numbers and dates follow the language
            return locale;
          },
          theme: AppTheme.light(),
          home: const _Root(),
        ),
      );
}

class _Root extends StatelessWidget {
  const _Root();

  @override
  Widget build(BuildContext context) =>
      AppScope.of(context).signedIn ? const MainShell() : const LoginScreen();
}
