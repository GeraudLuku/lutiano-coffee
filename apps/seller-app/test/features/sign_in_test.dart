import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/app.dart';
import 'package:lutiano_seller/state/app_state.dart';

import '../support/fake_api.dart';

const _operation = {
  'id': 'op1',
  'name': 'Bonapriso Cafe',
  'code': 'LTN-001',
  'type': 'FIXED_POS',
  'active': true,
  'city': 'Douala'
};

const _period = {
  'orders': 3,
  'sales_xaf': 12500,
  'customers': 1,
  'points_issued': 40,
  'commission_xaf': 625
};

FakeApi fakeApi({required String role}) => FakeApi({
      'POST /auth/login': (r) => (
            200,
            {
              'token': 't',
              'expiresAt': '2030-01-01T00:00:00Z',
              'user': {
                'id': 'u1',
                'email': 's@x',
                'full_name': 'Martin Nguemaleu',
                'role': role,
                'operations': [_operation]
              },
            }
          ),
      'GET /operations': (_) => (200, FakeApi.page([_operation])),
      'GET /operations/op1': (_) => (
            200,
            {
              ..._operation,
              'stats': {
                'today_xaf': 0,
                'week_xaf': 0,
                'month_xaf': 0,
                'transactions': 0
              }
            }
          ),
      'GET /shifts/open': (_) => (200, []),
      'GET /sales': (_) => (200, FakeApi.page([])),
      'GET /me/summary': (_) => (200, {'today': _period, 'month': _period}),
      'GET /inventory': (_) => (200, []),
      'GET /inventory/allocations/mine': (_) => (200, FakeApi.page([])),
    });

void main() {
  testWidgets('a seller signs in and lands on Home', (tester) async {
    final api = fakeApi(role: 'SELLER');
    await tester.pumpWidget(SellerApp(state: AppState(api: api.client())));
    await tester.enterText(
        find.byType(EditableText).at(0), 'seller@lutiano.local');
    await tester.enterText(find.byType(EditableText).at(1), 'Coffee123!');
    await tester.tap(find.text('Sign In'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Martin'), findsWidgets);
    expect(
        api.calls,
        containsAll([
          'POST /auth/login',
          'GET /operations',
          'GET /shifts/open',
          'GET /me/summary'
        ]));
    // the seller's own day, from the API (not the location's, and nothing hardcoded)
    expect(find.text('12,500 XAF'), findsOneWidget);
    expect(find.text('625 XAF'), findsOneWidget);
    expect(find.text('40 pts'), findsOneWidget);
  });

  testWidgets('a staff account is refused with a clear message',
      (tester) async {
    await tester.pumpWidget(
        SellerApp(state: AppState(api: fakeApi(role: 'ADMIN').client())));
    await tester.enterText(
        find.byType(EditableText).at(0), 'admin@lutiano.local');
    await tester.enterText(find.byType(EditableText).at(1), 'Coffee123!');
    await tester.tap(find.text('Sign In'));
    await tester.pumpAndSettle();
    expect(find.textContaining('This app is for sellers'), findsOneWidget);
  });
}
