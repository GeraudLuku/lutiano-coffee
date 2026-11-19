import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

class AuthRepository {
  AuthRepository(this._api);
  final ApiClient _api;

  /// Returns the session token and the signed-in profile.
  Future<(String, UserProfile)> login(String email, String password) async {
    final j = (await _api.post('/auth/login',
            body: {'email': email, 'password': password}) as Map)
        .cast<String, dynamic>();
    return (
      asString(j['token']),
      UserProfile.fromJson((j['user'] as Map).cast<String, dynamic>())
    );
  }

  Future<UserProfile> me() async => UserProfile.fromJson(
      (await _api.get('/auth/me') as Map).cast<String, dynamic>());

  Future<void> changePassword(String current, String next) =>
      _api.post('/auth/password',
          body: {'currentPassword': current, 'newPassword': next});

  /// Sends a code by SMS to the phone on file. The answer is the same for every email, so it never reveals who has an account.
  Future<void> forgotPassword(String email) =>
      _api.post('/auth/password/forgot', body: {'email': email.trim()});

  Future<void> resetPassword(String email, String code, String newPassword) =>
      _api.post('/auth/password/reset', body: {
        'email': email.trim(),
        'code': code.trim(),
        'newPassword': newPassword
      });
}
