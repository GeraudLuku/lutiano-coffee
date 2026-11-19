import 'package:intl/intl.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import 'core/l10n.dart';
import 'features/auth/login_screen.dart';
import 'features/auth/welcome_screen.dart';
import 'features/notifications/notification_target.dart';
import 'features/notifications/notifications_screen.dart';
import 'features/shell/main_shell.dart';
import 'state/app_scope.dart';
import 'state/app_state.dart';

class CustomerApp extends StatefulWidget {
  const CustomerApp({super.key, required this.state});
  final AppState state;

  @override
  State<CustomerApp> createState() => _CustomerAppState();
}

class _CustomerAppState extends State<CustomerApp> with WidgetsBindingObserver {
  final _navigator = GlobalKey<NavigatorState>();
  bool _wasSignedIn = false;
  late final PushService _push = PushService(
      register: widget.state.account.registerDevice,
      unregister: widget.state.account.unregisterDevice,
      onForeground: () {
        widget.state.refreshUnread();
        widget.state.ordersChanged();
        widget.state.loyaltyChanged();
      },
      onReady: () {
        widget.state.pushRegistered = true;
        widget.state.refreshUnread();
      },
      // a tapped notification is about an order (for now): show the orders
      onOpen: _openNotification);

  /// A tapped push notification opens what it is about (an order, a reward, a reply), or the inbox.
  Future<void> _openNotification(String id) async {
    final state = widget.state;
    state.ordersChanged();
    state.loyaltyChanged();
    try {
      final n = await state.inbox.notification(id);
      state.inbox
          .markRead([id])
          .then((_) => state.refreshUnread())
          .catchError((_) {});
      final screen = notificationScreen(n);
      _navigator.currentState?.push(MaterialPageRoute(
          builder: (_) => screen ?? const NotificationsScreen()));
    } catch (_) {
      _navigator.currentState?.push(
          MaterialPageRoute(builder: (_) => const NotificationsScreen()));
    }
  }

  @override
  void initState() {
    super.initState();
    widget.state.addListener(_onStateChanged);
    widget.state.beforeSignOut = _push.stop;
    _wasSignedIn = widget.state.signedIn;
    if (_wasSignedIn) _push.start();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    widget.state.removeListener(_onStateChanged);
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// Coming back to the app: look again for news.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && widget.state.signedIn) {
      widget.state.refreshUnread();
    }
  }

  void _onStateChanged() {
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
        child: ListenableBuilder(
          listenable: widget.state.settings,
          builder: (context, _) => MaterialApp(
            navigatorKey: _navigator,
            onGenerateTitle: (context) => context.l10n.appTitle,
            debugShowCheckedModeBanner: false,
            scrollBehavior: const AppScrollBehavior(),
            builder: (context, child) =>
                MediaUrls(toUrl: widget.state.api.mediaUrl, child: child!),
            localizationsDelegates: const [
              AppLocalizations.delegate,
              CoreLocalizations.delegate,
              ...GlobalMaterialLocalizations.delegates
            ],
            supportedLocales: AppLocalizations.supportedLocales,
            locale: widget.state.settings.locale,
            // French is the default; the phone's language is used when it is French or English.
            localeResolutionCallback: (device, supported) {
              final locale = supported.firstWhere(
                  (l) => l.languageCode == device?.languageCode,
                  orElse: () => const Locale('fr'));
              Intl.defaultLocale = locale.languageCode;
              return locale;
            },
            theme: AppTheme.light(),
            home: const _Root(),
          ),
        ),
      );
}

class _Root extends StatelessWidget {
  const _Root();

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    if (app.signedIn) return const MainShell();
    return app.settings.seenWelcome
        ? const LoginScreen()
        : const WelcomeScreen();
  }
}
