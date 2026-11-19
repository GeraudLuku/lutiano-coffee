import 'package:lutiano_core/lutiano_core.dart';

import '../models.dart';

/// The signed-in customer's own account: profile, favourites, notifications and devices.
class AccountRepository {
  AccountRepository(this._api);
  final ApiClient _api;

  Map<String, dynamic> _map(Object? d) => (d as Map).cast<String, dynamic>();

  Future<Customer> me() async =>
      Customer.fromJson(_map(await _api.get('/customer/me')));

  Future<Customer> update(
          {required String fullName,
          required String language,
          String? email,
          String? birthDate,
          bool marketingConsent = false}) async =>
      Customer.fromJson(_map(await _api.put('/customer/me', body: {
        'fullName': fullName,
        'language': language,
        'email': email,
        'birthDate': birthDate,
        'marketingConsent': marketingConsent,
      })));

  Future<void> startPhoneChange(String phone) =>
      _api.post('/customer/me/phone', body: {'phone': phone});

  Future<Customer> confirmPhoneChange(String code) async =>
      Customer.fromJson(_map(
          await _api.post('/customer/me/phone/confirm', body: {'code': code})));

  Future<void> changePassword(String? current, String next) =>
      _api.post('/customer/me/password',
          body: {'currentPassword': current, 'newPassword': next});

  Future<Customer> setTwoFactor(bool on) async => Customer.fromJson(
      _map(await _api.put('/customer/me/two-factor', body: {'enabled': on})));

  /// Signs out every other device; the answer is this device's fresh token.
  Future<String> signOutEverywhere() async =>
      asString(_map(await _api.post('/customer/me/sessions/revoke'))['token']);

  Future<void> deleteAccount({String? password, String? code}) =>
      _api.delete('/customer/me', body: {
        if (password != null) 'password': password,
        if (code != null) 'code': code
      });

  Future<void> sendDeletionCode() => _api.post('/customer/me/delete/code');

  // ----- favourites -----

  Future<Set<String>> favouriteIds() async =>
      (await _api.getAll('/customer/favourites'))
          .map((j) => asString(j['id']))
          .toSet();

  Future<Paged<MenuItem>> favourites({int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/customer/favourites', page: page, size: size),
          MenuItem.fromJson);

  Future<void> addFavourite(String productId) =>
      _api.put('/customer/favourites/$productId');

  Future<void> removeFavourite(String productId) =>
      _api.delete('/customer/favourites/$productId');

  // ----- notifications -----

  Future<int> unreadCount() async => asInt(
      _map(await _api.get('/customer/notifications/unread-count'))['unread']);

  Future<void> registerDevice(String token) =>
      _api.post('/customer/notifications/devices',
          body: {'token': token, 'platform': 'ANDROID'});

  Future<void> unregisterDevice(String token) =>
      _api.delete('/customer/notifications/devices', query: {'token': token});
}
