import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart' show Locale;
import 'package:shared_preferences/shared_preferences.dart';

/// Choices kept on this phone: the app language (null follows the phone) and the sales points the customer visited lately.
/// Without [prefs] (tests) they only last while the app runs.
class AppSettings extends ChangeNotifier {
  AppSettings([this._prefs]);
  final SharedPreferences? _prefs;

  static const _language = 'language',
      _recent = 'recentSalesPoints',
      _welcomed = 'welcomed';

  static Future<AppSettings> load() async =>
      AppSettings(await SharedPreferences.getInstance());

  String? _lang;
  List<Map<String, String>>? _recentPoints;
  bool? _seenWelcome;

  String? get languageCode => _lang ?? _prefs?.getString(_language);
  Locale? get locale => languageCode == null ? null : Locale(languageCode!);

  Future<void> setLanguage(String? code) async {
    _lang = code;
    code == null
        ? await _prefs?.remove(_language)
        : await _prefs?.setString(_language, code);
    if (code == null) _lang = null;
    notifyListeners();
  }

  /// Whether the welcome slides were shown already.
  bool get seenWelcome => _seenWelcome ?? _prefs?.getBool(_welcomed) ?? false;

  Future<void> markWelcomeSeen() async {
    _seenWelcome = true;
    await _prefs?.setBool(_welcomed, true);
  }

  /// The last sales points opened, newest first: {id, name}.
  List<Map<String, String>> get recentSalesPoints {
    if (_recentPoints != null) return _recentPoints!;
    final raw = _prefs?.getString(_recent);
    if (raw == null) return const [];
    try {
      return [
        for (final e in jsonDecode(raw) as List)
          (e as Map).cast<String, String>()
      ];
    } catch (_) {
      return const [];
    }
  }

  Future<void> rememberSalesPoint(String id, String name) async {
    final list = [
      {'id': id, 'name': name},
      ...recentSalesPoints.where((p) => p['id'] != id)
    ].take(5).toList();
    _recentPoints = list;
    await _prefs?.setString(_recent, jsonEncode(list));
    notifyListeners();
  }
}
