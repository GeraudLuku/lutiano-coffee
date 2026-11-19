import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:lutiano_core/testing.dart';
import 'package:lutiano_customer/state/app_state.dart';

import '../support/fakes.dart';

class _Store implements TokenStore {
  _Store([this.token]);
  String? token;
  @override
  Future<String?> read() async => token;
  @override
  Future<void> write(String t) async => token = t;
  @override
  Future<void> clear() async => token = null;
}

void main() {
  test('a saved token signs the customer back in', () async {
    final api = FakeApi({
      'GET /customer/me': (_) => (200, customerJson()),
      'GET /customer/notifications/unread-count': (_) => (200, {'unread': 2}),
      'GET /customer/favourites': (_) => (
            200,
            FakeApi.page([
              {'id': 'p1'}
            ])
          ),
    });
    final state = AppState(api: api.client(), tokenStore: _Store('saved'));
    await state.restoreSession();
    await Future<void>.delayed(Duration.zero);
    expect(state.signedIn, isTrue);
    expect(state.isFavourite('p1'), isTrue);
    expect(state.unreadNotifications, 2);
  });

  test('an expired token is dropped and signing out forgets it', () async {
    final api = FakeApi({
      'GET /customer/me': (_) => (
            401,
            {
              'error': {'message': 'expired'}
            }
          )
    });
    final store = _Store('old');
    final state = AppState(api: api.client(), tokenStore: store);
    await state.restoreSession();
    expect(state.signedIn, isFalse);
    expect(store.token, isNull);
    store.token = 'x';
    state.signOut();
    expect(store.token, isNull);
  });
}
