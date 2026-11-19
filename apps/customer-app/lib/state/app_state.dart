import 'package:flutter/foundation.dart';
import 'package:lutiano_core/lutiano_core.dart';

import '../core/config.dart';
import '../data/models.dart';
import '../data/repositories.dart';
import 'app_settings.dart';
import 'cart_controller.dart';

/// Who is signed in, their favourites and cart. Browsing the menu needs no account.
class AppState extends ChangeNotifier {
  /// [tokenStore] keeps the session between launches; without it (tests) a session only lasts while the app runs.
  AppState({ApiClient? api, AppSettings? settings, this.tokenStore})
      : api = api ??
            ApiClient(
                baseUrl: AppConfig.apiBase, timeout: AppConfig.requestTimeout),
        settings = settings ?? AppSettings() {
    this.settings.addListener(notifyListeners);
    cart.addListener(
        notifyListeners); // the cart badges on Home, Menu and the tab bar follow the cart
    auth = AuthRepository(this.api);
    catalogue = CatalogueRepository(this.api);
    account = AccountRepository(this.api);
    orders = OrdersRepository(this.api);
    loyalty = LoyaltyRepository(this.api);
    inbox = InboxRepository(this.api);
    this.api.onSessionExpired = () => signOut(expired: true);
  }

  final ApiClient api;
  final TokenStore? tokenStore;
  final AppSettings settings;
  late final AuthRepository auth;
  late final CatalogueRepository catalogue;
  late final AccountRepository account;
  late final OrdersRepository orders;
  late final LoyaltyRepository loyalty;
  late final InboxRepository inbox;
  final CartController cart = CartController();

  Customer? customer;

  /// The sales point the customer is ordering from (the cart belongs to it); null until one is chosen or scanned.
  SalesPoint? salesPoint;
  bool sessionExpired = false;
  Set<String> _favourites = {};

  /// Unread notifications (the bell's badge).
  int unreadNotifications = 0;

  /// Changes when an order changes (a push arrived, or the customer acted), so order lists reload.
  int ordersVersion = 0;

  /// A tab a notification asks the app to open (see MainShell); taken once.
  int? _pendingTab;

  /// Changes when points or the wallet change (a redemption, an order that earned points), so the Rewards screens reload.
  int loyaltyVersion = 0;

  void loyaltyChanged() {
    loyaltyVersion++;
    notifyListeners();
  }

  void ordersChanged() {
    ordersVersion++;
    notifyListeners();
  }

  void openTab(int tab) {
    _pendingTab = tab;
    notifyListeners();
  }

  int? takePendingTab() {
    final t = _pendingTab;
    _pendingTab = null;
    return t;
  }

  /// True once this phone is registered for push notifications.
  bool pushRegistered = false;

  /// Called first on sign-out, so the phone stops receiving this person's notifications.
  VoidCallback? beforeSignOut;

  bool get signedIn => customer != null;

  /// Chooses where the customer orders from. Another sales point starts an empty cart.
  void selectSalesPoint(SalesPoint point) {
    salesPoint = point;
    cart.bindTo(point.id);
    settings.rememberSalesPoint(point.id, point.name);
    notifyListeners();
  }

  /// A sign-in step succeeded: keep the token and load what the app shows.
  Future<void> startSession(SignedIn step) async {
    api.token = step.token;
    tokenStore?.write(step.token);
    customer = step.customer;
    sessionExpired = false;
    notifyListeners();
    _loadAccountData();
  }

  /// Signs the customer back in from the token kept on this phone, if it is still valid.
  Future<void> restoreSession() async {
    final saved = await tokenStore?.read();
    if (saved == null) return;
    api.token = saved;
    try {
      customer = await account.me();
      _loadAccountData();
    } catch (e) {
      api.token = null;
      customer = null;
      sessionExpired = false;
      if (!(e is ApiException && e.isNetwork)) tokenStore?.clear();
    }
    notifyListeners();
  }

  void _loadAccountData() {
    refreshUnread();
    account.favouriteIds().then((ids) {
      _favourites = ids;
      notifyListeners();
    }).catchError((_) {});
  }

  void replaceToken(String token) {
    api.token = token;
    tokenStore?.write(token);
  }

  void signOut({bool expired = false}) {
    if (!expired) beforeSignOut?.call();
    pushRegistered = false;
    api.token = null;
    tokenStore?.clear();
    customer = null;
    _favourites = {};
    unreadNotifications = 0;
    cart.clear();
    salesPoint = null;
    sessionExpired = expired;
    notifyListeners();
  }

  Future<void> reloadProfile() async {
    customer = await account.me();
    notifyListeners();
  }

  Future<void> refreshUnread() async {
    if (!signedIn) return;
    try {
      unreadNotifications = await account.unreadCount();
      notifyListeners();
    } catch (_) {
      // the badge is a convenience: never let it break a screen
    }
  }

  bool isFavourite(String productId) => _favourites.contains(productId);

  /// Changes whenever a heart is added or removed, so the favourites list reloads.
  int get favouriteCount => _favourites.length;

  /// Hearts or un-hearts a product; the heart changes at once and goes back if the server refuses.
  Future<void> toggleFavourite(String productId) async {
    final was = isFavourite(productId);
    was ? _favourites.remove(productId) : _favourites.add(productId);
    notifyListeners();
    try {
      was
          ? await account.removeFavourite(productId)
          : await account.addFavourite(productId);
    } catch (e) {
      was ? _favourites.add(productId) : _favourites.remove(productId);
      notifyListeners();
      rethrow;
    }
  }
}
