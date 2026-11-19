import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Keeps the session token in the phone's secure storage (Keychain on iOS, Keystore-backed on Android), so a seller stays signed in
/// between launches. Storage failures never break the app: the seller simply signs in again.
class TokenStore {
  const TokenStore([this._storage = const FlutterSecureStorage()]);
  final FlutterSecureStorage _storage;
  static const _key = 'session_token';

  Future<String?> read() async {
    try {
      return await _storage.read(key: _key);
    } catch (_) {
      return null;
    }
  }

  Future<void> write(String token) async {
    try {
      await _storage.write(key: _key, value: token);
    } catch (_) {/* signing in again is the fallback */}
  }

  Future<void> clear() async {
    try {
      await _storage.delete(key: _key);
    } catch (_) {/* nothing to clean */}
  }
}
