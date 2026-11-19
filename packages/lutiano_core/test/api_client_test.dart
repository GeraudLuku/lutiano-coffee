import 'package:flutter_test/flutter_test.dart';
import 'package:intl/intl.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:lutiano_core/lutiano_core.dart';

import 'package:lutiano_core/testing.dart';

void main() {
  setUp(() => Intl.defaultLocale = 'en');
  test('unwraps the GenericResponse envelope', () async {
    final api = FakeApi({
      'GET /ping': (_) => (200, {'ok': true})
    }).client();
    expect(await api.get('/ping'), {'ok': true});
  });

  test('sends the bearer token once signed in', () async {
    String? header;
    final api = ApiClient(
        baseUrl: 'http://localhost/api/v1',
        client: MockClient((r) async {
          header = r.headers['Authorization'];
          return http.Response('{"success":true,"data":null}', 200);
        }))
      ..token = 'abc';
    await api.get('/x');
    expect(header, 'Bearer abc');
  });

  test('turns an error body into a readable message', () async {
    final api = FakeApi({
      'GET /x': (_) => (
            400,
            {
              'success': false,
              'error': {'message': 'Quantity is required.'}
            }
          )
    }).client();
    expect(
        () => api.get('/x'),
        throwsA(isA<ApiException>()
            .having((e) => e.message, 'message', 'Quantity is required.')));
  });

  test('a rejected token signs the session out; a rejected sign-in does not',
      () async {
    var expired = 0;
    final signedIn = FakeApi({'GET /x': (_) => (401, {})}).client()
      ..token = 't'
      ..onSessionExpired = () => expired++;
    await expectLater(signedIn.get('/x'), throwsA(isA<ApiException>()));
    expect(expired, 1);

    final signingIn = FakeApi({
      'POST /auth/login': (_) => (
            401,
            {
              'error': {'message': 'Wrong email or password.'}
            }
          )
    }).client()
      ..onSessionExpired = () => expired++;
    await expectLater(
        signingIn.post('/auth/login'), throwsA(isA<ApiException>()));
    expect(expired, 1);
  });

  test('an unreachable server is a network error with a friendly message',
      () async {
    final api = ApiClient(
        baseUrl: 'http://localhost/api/v1',
        client: MockClient((_) async => throw http.ClientException('down')));
    expect(
        () => api.get('/x'),
        throwsA(
            isA<ApiException>().having((e) => e.isNetwork, 'isNetwork', true)));
  });

  test('getAll follows every page', () async {
    final api = FakeApi({
      'GET /things': (r) {
        final page = int.parse(r.url.queryParameters['page']!);
        return (
          200,
          {
            'items': [
              {'n': page}
            ],
            'page': page,
            'size': 200,
            'total': 2,
            'pages': 2
          }
        );
      },
    }).client();
    expect((await api.getAll('/things')).map((r) => r['n']), [1, 2]);
  });

  test('errorMessage shows the API message and hides raw exceptions', () {
    expect(errorMessage(const ApiException(400, 'Quantity is required.')),
        'Quantity is required.');
    expect(errorMessage(const FormatException('Unexpected character at 0')),
        'Something went wrong. Try again.');
    expect(errorMessage(StateError('boom')), isNot(contains('boom')));
    Intl.defaultLocale = 'fr';
    expect(errorMessage(StateError('boom')),
        'Une erreur est survenue. Réessayez.');
  });
}
