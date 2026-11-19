import 'package:flutter/foundation.dart';

import 'package:lutiano_core/lutiano_core.dart';

import '../core/config.dart';
import '../data/models.dart';
import '../data/repositories.dart';
import 'app_settings.dart';
import 'cart_controller.dart';

/// Session and working context: who is signed in, which operation they are selling for, and their cash shift.
class AppState extends ChangeNotifier {
  /// [tokenStore] keeps the session between launches; without it (tests) a session only lasts while the app runs.
  AppState({ApiClient? api, AppSettings? settings, this.tokenStore})
      : api = api ??
            ApiClient(
                baseUrl: AppConfig.apiBase, timeout: AppConfig.requestTimeout),
        settings = settings ?? AppSettings() {
    this.settings.addListener(notifyListeners);
    auth = AuthRepository(this.api);
    catalog = CatalogRepository(this.api);
    inventory = InventoryRepository(this.api);
    sales = SalesRepository(this.api);
    shifts = ShiftRepository(this.api);
    cash = CashRepository(this.api);
    customers = CustomerRepository(this.api);
    requests = RequestsRepository(this.api);
    earnings = EarningsRepository(this.api);
    targets = TargetsRepository(this.api);
    campaigns = CampaignsRepository(this.api);
    orders = OrdersRepository(this.api);
    rewards = RewardsRepository(this.api);
    notifications = NotificationsRepository(this.api);
    account = AccountRepository(this.api);
    support = SupportRepository(this.api);
    this.api.onSessionExpired = () => signOut(expired: true);
  }

  final ApiClient api;
  final TokenStore? tokenStore;
  late final AuthRepository auth;
  late final CatalogRepository catalog;
  late final InventoryRepository inventory;
  late final SalesRepository sales;
  late final ShiftRepository shifts;
  late final CashRepository cash;
  late final CustomerRepository customers;
  late final RequestsRepository requests;
  late final EarningsRepository earnings;
  late final TargetsRepository targets;
  late final CampaignsRepository campaigns;
  late final OrdersRepository orders;
  late final RewardsRepository rewards;
  late final NotificationsRepository notifications;
  late final AccountRepository account;
  late final SupportRepository support;

  /// Choices kept on this phone (language, receipt, default operation).
  final AppSettings settings;
  final CartController cart = CartController();

  UserProfile? user;
  List<Operation> operations = const [];
  Operation? operation;
  CashShift? shift;
  bool sessionExpired = false;

  /// Bumped after any change that affects lists (sale completed, stock confirmed); screens key their loaders on it.
  int dataVersion = 0;

  /// How many notifications are unread (the bell's badge); refreshed at sign-in, when the app comes back, and after reading.
  int unreadNotifications = 0;

  /// True once this phone is registered for push notifications (Android, with Firebase); the settings then offer the push switches.
  bool pushRegistered = false;

  /// Called first when someone signs out, so the phone stops receiving their notifications (it must start its request before the token goes).
  void Function()? beforeSignOut;

  bool get signedIn => user != null;

  Future<void> refreshUnread() async {
    try {
      unreadNotifications = await notifications.unreadCount();
      notifyListeners();
    } catch (_) {
      // the badge is a convenience: never let it break a screen
    }
  }

  Future<void> signIn(String email, String password) async {
    final (token, profile) = await auth.login(email.trim(), password);
    if (!profile.isSeller) {
      throw const ApiException(403,
          'This app is for sellers. Staff accounts use the LUTIANO Admin Portal.');
    }
    api.token = token;
    tokenStore?.write(token);
    user = profile;
    sessionExpired = false;
    await refreshContext();
    refreshUnread(); // not awaited: the badge fills in when it arrives
  }

  /// Signs the seller back in from the token kept on this phone, if it is still valid; otherwise the sign-in screen shows.
  Future<void> restoreSession() async {
    final saved = await tokenStore?.read();
    if (saved == null) return;
    api.token = saved;
    try {
      final profile = await auth.me();
      if (!profile.isSeller) throw const ApiException(403, 'Not a seller');
      user = profile;
      await refreshContext();
      refreshUnread();
    } catch (e) {
      // expired or revoked (the client already signed out on a 401), or no network: start from the sign-in screen
      api.token = null;
      user = null;
      sessionExpired = false;
      if (!(e is ApiException && e.isNetwork)) tokenStore?.clear();
    }
    notifyListeners();
  }

  /// Another token for this phone (after signing out every other device).
  void replaceToken(String token) {
    api.token = token;
    tokenStore?.write(token);
  }

  void signOut({bool expired = false}) {
    if (!expired) beforeSignOut?.call();
    pushRegistered = false;
    api.token = null;
    tokenStore?.clear();
    user = null;
    operations = const [];
    operation = null;
    shift = null;
    sessionExpired = expired;
    unreadNotifications = 0;
    cart.clear();
    notifyListeners();
  }

  /// Reloads assigned operations and the open shift; keeps the selected operation if still assigned.
  Future<void> refreshContext() async {
    final results =
        await Future.wait([catalog.myOperations(), shifts.current()]);
    operations = results[0] as List<Operation>;
    shift = results[1] as CashShift?;
    final keep = operation == null
        ? null
        : operations.where((o) => o.id == operation!.id).firstOrNull;
    final preferred = operations
        .where((o) => o.id == settings.defaultOperationId)
        .firstOrNull;
    selectOperation(keep ?? preferred ?? operations.firstOrNull);
  }

  void selectOperation(Operation? op) {
    operation = op;
    if (op != null) cart.bindTo(op.id);
    notifyListeners();
  }

  void dataChanged() {
    dataVersion++;
    notifyListeners();
  }

  Future<void> refreshShift() async {
    shift = await shifts.current();
    notifyListeners();
  }

  Future<void> openShift(int openingFloat) async {
    await shifts.open(openingFloat);
    await refreshShift();
  }

  Future<ShiftCloseResult> closeShift(int countedCash, String? reason) async {
    final result = await shifts.close(countedCash, reason);
    await refreshShift();
    return result;
  }
}
