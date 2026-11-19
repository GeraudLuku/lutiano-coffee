import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:lutiano_seller/state/app_state.dart';

import '../support/fake_api.dart';
import '../support/seller_fakes.dart';

class _MemoryStore implements TokenStore {
  _MemoryStore([this.token]);
  String? token;
  @override
  Future<String?> read() async => token;
  @override
  Future<void> write(String t) async => token = t;
  @override
  Future<void> clear() async => token = null;
}

Map<String, Object?> get _me => {
      'id': 'u1',
      'email': 's@x.local',
      'full_name': 'Sandra Eto',
      'role': 'SELLER',
      'seller_role': 'MOBILE_BARISTA',
      'operations': [operationJson],
    };

void main() {
  test('a saved token signs the seller back in', () async {
    final api = FakeApi({
      'GET /auth/me': (_) => (200, _me),
      'GET /operations': (_) => (200, FakeApi.page([operationJson])),
      'GET /shifts/open': (_) => (200, <Object?>[]),
      'GET /me/notifications/unread-count': (_) => (200, {'unread': 0}),
    });
    final store = _MemoryStore('saved');
    final state = AppState(api: api.client(), tokenStore: store);
    await state.restoreSession();
    expect(state.signedIn, isTrue);
    expect(state.api.token, 'saved');
  });

  test('an expired token is dropped and the seller starts at sign-in',
      () async {
    final api = FakeApi({
      'GET /auth/me': (_) => (
            401,
            {
              'error': {'message': 'expired'}
            }
          ),
    });
    final store = _MemoryStore('old');
    final state = AppState(api: api.client(), tokenStore: store);
    await state.restoreSession();
    expect(state.signedIn, isFalse);
    expect(state.sessionExpired, isFalse);
    expect(store.token, isNull);
  });

  test('signing out forgets the token', () async {
    final store = _MemoryStore('x');
    final state = AppState(api: FakeApi({}).client(), tokenStore: store);
    state.signOut();
    expect(store.token, isNull);
  });

  test('no saved token means the sign-in screen', () async {
    final state =
        AppState(api: FakeApi({}).client(), tokenStore: _MemoryStore());
    await state.restoreSession();
    expect(state.signedIn, isFalse);
  });
}
