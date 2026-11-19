import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import '../core/api_client.dart';

/// A scripted API: each route is "METHOD /path" (no base) mapped to a handler returning a status and a JSON body.
/// Anything not scripted is a 404, so a test fails loudly when the app calls something unexpected.
class FakeApi {
  FakeApi(this.routes);

  final Map<String, (int, Object?) Function(http.Request)> routes;
  final List<String> calls = [];

  /// Every request's full URL (path and query), in order.
  final List<Uri> uris = [];

  ApiClient client() => ApiClient(
        baseUrl: 'http://localhost/api/v1',
        client: MockClient((request) async {
          final key =
              '${request.method} ${request.url.path.replaceFirst('/api/v1', '')}';
          calls.add(key);
          uris.add(request.url);
          final handler = routes[key];
          if (handler == null) {
            return http.Response(
                jsonEncode({
                  'success': false,
                  'error': {'message': 'No route $key'}
                }),
                404);
          }
          final (status, body) = handler(request);
          return http.Response(
              jsonEncode(status < 300 ? {'success': true, 'data': body} : body),
              status,
              headers: {'content-type': 'application/json'});
        }),
      );

  /// A Spring-style page envelope.
  static Map<String, Object?> page(List<Object?> items) => {
        'items': items,
        'page': 1,
        'size': 200,
        'total': items.length,
        'pages': 1
      };
}
