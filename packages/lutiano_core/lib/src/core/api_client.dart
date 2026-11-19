import 'dart:convert';

import 'dart:ui' show Locale;

import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart' show MediaType;
import 'package:intl/intl.dart';

import '../l10n/core_localizations.dart';

/// Error with a message that is safe to show to the user.
class ApiException implements Exception {
  const ApiException(this.status, this.message);

  /// 0 means the server could not be reached.
  final int status;
  final String message;

  bool get isNetwork => status == 0;

  @override
  String toString() => message;
}

/// The text to show a person for any failure: the server's or network message for an [ApiException], otherwise a generic line
/// (never the raw exception text, which can expose internals).
String errorMessage(Object error) =>
    error is ApiException ? error.message : _texts().errUnexpected;

/// The shared translated texts for code that has no BuildContext (the language the app resolved at start-up).
CoreLocalizations _texts() =>
    lookupCoreLocalizations(Locale(Intl.defaultLocale ?? 'fr'));

/// Thin JSON-over-HTTP client that adds the bearer token and turns failures into [ApiException].
class ApiClient {
  ApiClient(
      {required this.baseUrl,
      this.timeout = const Duration(seconds: 20),
      http.Client? client})
      : _http = client ?? http.Client();

  /// The API's root address, e.g. https://api.example.com/api/v1.
  final String baseUrl;
  final Duration timeout;
  final http.Client _http;
  String? token;

  /// Called when an authenticated request is rejected (expired or revoked session).
  void Function()? onSessionExpired;

  Map<String, String> get authHeaders =>
      token == null ? const {} : {'Authorization': 'Bearer $token'};

  Uri uri(String path, [Map<String, String>? query]) {
    final base = Uri.parse('$baseUrl$path');
    return query == null || query.isEmpty
        ? base
        : base.replace(queryParameters: query);
  }

  /// Product images are served by the API; older records may hold absolute URLs.
  String? mediaUrl(String? url) {
    if (url == null || url.isEmpty) return null;
    return url.startsWith('http') ? url : '$baseUrl$url';
  }

  Future<dynamic> get(String path, {Map<String, String>? query}) =>
      _json('GET', path, query: query);

  Future<dynamic> post(String path,
          {Object? body, Map<String, String>? headers}) =>
      _json('POST', path, body: body ?? const {}, headers: headers);

  Future<dynamic> delete(String path,
          {Map<String, String>? query, Object? body}) =>
      _json('DELETE', path, query: query, body: body);

  /// Sends a multipart form (text [fields] and an optional file) and returns the response data.
  Future<dynamic> upload(String path,
      {Map<String, String> fields = const {},
      String fileField = 'file',
      List<int>? bytes,
      String? fileName,
      String? contentType}) async {
    final request = http.MultipartRequest('POST', uri(path, null))
      ..headers.addAll({'Accept': 'application/json', ...authHeaders})
      ..fields.addAll(fields);
    if (bytes != null) {
      request.files.add(http.MultipartFile.fromBytes(fileField, bytes,
          filename: fileName ?? 'upload',
          contentType:
              contentType == null ? null : MediaType.parse(contentType)));
    }
    final http.Response resp;
    try {
      resp = await http.Response.fromStream(
          await _http.send(request).timeout(timeout));
    } on Exception {
      throw ApiException(0, _texts().errNetwork);
    }
    if (resp.statusCode < 200 || resp.statusCode >= 300) throw _toError(resp);
    if (resp.body.isEmpty) return null;
    final decoded = jsonDecode(resp.body);
    return decoded is Map && decoded.containsKey('success')
        ? decoded['data']
        : decoded;
  }

  Future<dynamic> put(String path, {Object? body}) =>
      _json('PUT', path, body: body ?? const {});

  Future<List<int>> bytes(String path) async =>
      (await _send('GET', path)).bodyBytes;

  /// Every JSON response is a GenericResponse envelope `{success, data, error, timestamp}`; callers get `data`.
  Future<dynamic> _json(String method, String path,
      {Object? body,
      Map<String, String>? query,
      Map<String, String>? headers}) async {
    final resp =
        await _send(method, path, body: body, query: query, headers: headers);
    if (resp.body.isEmpty) return null;
    final decoded = jsonDecode(resp.body);
    return decoded is Map && decoded.containsKey('success')
        ? decoded['data']
        : decoded;
  }

  /// One page of a list endpoint: `{items, page, size, total, pages, summary}`.
  Future<Map<String, dynamic>> page(String path,
          {Map<String, String>? query, int page = 1, int size = 5}) async =>
      ((await get(path, query: {...?query, 'page': '$page', 'size': '$size'}))
              as Map)
          .cast<String, dynamic>();

  /// Every row of a list endpoint, fetched page by page (for short lists the screen needs whole, e.g. my locations).
  Future<List<Map<String, dynamic>>> getAll(String path,
      {Map<String, String>? query, int limit = 2000}) async {
    final out = <Map<String, dynamic>>[];
    for (var p = 1;; p++) {
      final result = await page(path, query: query, page: p, size: 200);
      out.addAll((result['items'] as List).cast<Map<String, dynamic>>());
      if (p >= (result['pages'] as num).toInt() || out.length >= limit) {
        return out;
      }
    }
  }

  Future<http.Response> _send(String method, String path,
      {Object? body,
      Map<String, String>? query,
      Map<String, String>? headers}) async {
    final request = http.Request(method, uri(path, query))
      ..headers
          .addAll({'Accept': 'application/json', ...authHeaders, ...?headers});
    if (body != null) {
      request.headers['Content-Type'] = 'application/json';
      request.body = jsonEncode(body);
    }
    final http.Response resp;
    try {
      resp = await http.Response.fromStream(
          await _http.send(request).timeout(timeout));
    } on Exception {
      throw ApiException(0, _texts().errNetwork);
    }
    if (resp.statusCode >= 200 && resp.statusCode < 300) return resp;
    throw _toError(resp);
  }

  ApiException _toError(http.Response r) {
    if (r.statusCode == 401 && token != null) onSessionExpired?.call();
    var message = switch (r.statusCode) {
      401 => _texts().errSessionExpired,
      403 => _texts().errForbidden,
      _ => _texts().errGeneric(r.statusCode),
    };
    try {
      final body = jsonDecode(r.body);
      if (body is Map) {
        final error = body['error'];
        if (error is Map && error['message'] is String) {
          message = error['message'] as String;
        } else if (body['message'] is String) {
          message = body['message'] as String;
        }
      }
    } catch (_) {
      // Not a JSON error body; keep the default message.
    }
    return ApiException(r.statusCode, message);
  }
}
