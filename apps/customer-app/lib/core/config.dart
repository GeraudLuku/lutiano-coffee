/// Build-time configuration. Override with:
/// flutter run --dart-define=LUTIANO_API=https://staging.example.com/api/v1
class AppConfig {
  AppConfig._();

  /// 10.0.2.2 is the host machine from the Android emulator.
  static const apiBase = String.fromEnvironment('LUTIANO_API',
      defaultValue: 'http://10.0.2.2:8080/api/v1');
  static const appVersion = '0.1.0';
  static const requestTimeout = Duration(seconds: 20);

  /// The OAuth web/server client id of the Google project (the API checks the token was issued for it).
  static const googleClientId =
      String.fromEnvironment('LUTIANO_GOOGLE_CLIENT_ID');
}
