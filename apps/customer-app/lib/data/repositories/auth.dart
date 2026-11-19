import 'package:lutiano_core/lutiano_core.dart';

import '../models.dart';

/// Sign-up, sign-in and password reset (all answered by the open /customer/auth endpoints).
class AuthRepository {
  AuthRepository(this._api);
  final ApiClient _api;

  Map<String, dynamic> _map(Object? d) => (d as Map).cast<String, dynamic>();

  Future<void> signUp(
          {required String fullName,
          required String phone,
          required String language,
          String? email,
          String? password}) =>
      _api.post('/customer/auth/signup', body: {
        'fullName': fullName,
        'phone': phone,
        'language': language,
        'acceptTerms': true,
        if (email != null && email.isNotEmpty) 'email': email,
        if (password != null && password.isNotEmpty) 'password': password,
      });

  Future<AuthStep> verifySignUp(String phone, String code) async =>
      AuthStep.fromJson(_map(await _api.post('/customer/auth/signup/verify',
          body: {'phone': phone, 'code': code})));

  Future<void> requestCode(String phone) =>
      _api.post('/customer/auth/otp/request', body: {'phone': phone});

  Future<AuthStep> verifyCode(String phone, String code) async =>
      AuthStep.fromJson(_map(await _api.post('/customer/auth/otp/verify',
          body: {'phone': phone, 'code': code})));

  Future<AuthStep> login(String phone, String password) async =>
      AuthStep.fromJson(_map(await _api.post('/customer/auth/login',
          body: {'phone': phone, 'password': password})));

  Future<AuthStep> verifySecondStep(String challenge, String code) async =>
      AuthStep.fromJson(_map(await _api.post('/customer/auth/login/verify',
          body: {'challenge': challenge, 'code': code})));

  Future<void> forgotPassword(String phone) =>
      _api.post('/customer/auth/password/forgot', body: {'phone': phone});

  Future<void> resetPassword(String phone, String code, String newPassword) =>
      _api.post('/customer/auth/password/reset',
          body: {'phone': phone, 'code': code, 'newPassword': newPassword});

  /// Which of Google and Apple sign-in the server has switched on.
  Future<({bool google, bool apple})> providers() async {
    final j = _map(await _api.get('/customer/auth/providers'));
    return (google: j['google'] == true, apple: j['apple'] == true);
  }

  Future<AuthStep> social(String provider, String idToken,
          {String? nonce}) async =>
      AuthStep.fromJson(_map(await _api.post('/customer/auth/social', body: {
        'provider': provider,
        'idToken': idToken,
        if (nonce != null) 'nonce': nonce
      })));

  Future<void> socialPhone(
          String ticket, String phone, String fullName, String language) =>
      _api.post('/customer/auth/social/phone', body: {
        'ticket': ticket,
        'phone': phone,
        'fullName': fullName,
        'acceptTerms': true,
        'language': language
      });

  Future<AuthStep> socialVerify(String ticket, String code) async =>
      AuthStep.fromJson(_map(await _api.post('/customer/auth/social/verify',
          body: {'ticket': ticket, 'code': code})));
}
