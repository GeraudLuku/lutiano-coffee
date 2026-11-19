import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart' show Locale;
import 'package:shared_preferences/shared_preferences.dart';

/// Choices the seller makes on this phone: the app language (null follows the phone), whether the receipt is offered after a sale,
/// and the operation to open by default. Kept on the device; without [prefs] (tests) they only last while the app runs.
class AppSettings extends ChangeNotifier {
  AppSettings([this._prefs]);
  final SharedPreferences? _prefs;

  static const _language = 'language',
      _receipt = 'showReceipt',
      _operation = 'defaultOperation';

  static Future<AppSettings> load() async =>
      AppSettings(await SharedPreferences.getInstance());

  String? _lang, _op;
  bool? _showReceipt;

  /// The chosen language code, or null to follow the phone.
  String? get languageCode => _lang ?? _prefs?.getString(_language);
  Locale? get locale => languageCode == null ? null : Locale(languageCode!);

  /// Whether the sale confirmation offers the receipt (on by default).
  bool get showReceipt => _showReceipt ?? _prefs?.getBool(_receipt) ?? true;

  /// The operation to open when signing in, if it is still assigned.
  String? get defaultOperationId => _op ?? _prefs?.getString(_operation);

  Future<void> setLanguage(String? code) async {
    _lang = code;
    code == null
        ? await _prefs?.remove(_language)
        : await _prefs?.setString(_language, code);
    if (code == null) _lang = null;
    notifyListeners();
  }

  Future<void> setShowReceipt(bool on) async {
    _showReceipt = on;
    await _prefs?.setBool(_receipt, on);
    notifyListeners();
  }

  Future<void> setDefaultOperation(String? id) async {
    _op = id;
    id == null
        ? await _prefs?.remove(_operation)
        : await _prefs?.setString(_operation, id);
    notifyListeners();
  }
}
