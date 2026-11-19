import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:lutiano_seller/features/sale/sale_customer_card.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';

const _customer = {
  'full_name': 'Mireille Bessala',
  'code': 'CU-000019',
  'tier_name': 'Bronze',
  'tier_color': '#CD7F32',
  'points_balance': 1520
};

Widget _host(AppState state, Future<String?> Function(BuildContext) onScan) =>
    AppScope(
      state: state,
      child: testApp(home: Scaffold(body: SaleCustomerCard(onScan: onScan))),
    );

void main() {
  testWidgets(
      'a scanned QR attaches the customer and keeps the token for the sale',
      (tester) async {
    final api = FakeApi({
      'GET /customers/scan': (r) =>
          r.url.queryParameters['token'] == 'good-token'
              ? (200, _customer)
              : (
                  400,
                  {
                    'error': {'message': 'This loyalty QR code is not valid.'}
                  }
                ),
    });
    final state = AppState(api: api.client())..cart.bindTo('op1');
    await tester.pumpWidget(_host(state, (_) async => 'good-token'));
    await tester.tap(find.text('Scan Customer QR'));
    await tester.pumpAndSettle();
    expect(find.text('Mireille Bessala'), findsOneWidget);
    expect(find.textContaining('1,520 pts'), findsOneWidget);
    expect(state.cart.customerToken, 'good-token');

    await tester.tap(find.byTooltip('Remove customer'));
    await tester.pumpAndSettle();
    expect(state.cart.customer, isNull);
    expect(find.text('Scan Customer QR'), findsOneWidget);
  });

  testWidgets('an invalid QR shows the server message and attaches nobody',
      (tester) async {
    final api = FakeApi({
      'GET /customers/scan': (_) => (
            400,
            {
              'error': {'message': 'This loyalty QR code is not valid.'}
            }
          )
    });
    final state = AppState(api: api.client())..cart.bindTo('op1');
    await tester.pumpWidget(_host(state, (_) async => 'bad'));
    await tester.tap(find.text('Scan Customer QR'));
    await tester.pumpAndSettle();
    expect(find.text('This loyalty QR code is not valid.'), findsOneWidget);
    expect(state.cart.customer, isNull);
  });

  testWidgets('backing out of the scanner changes nothing and calls no API',
      (tester) async {
    final api = FakeApi({});
    final state = AppState(api: api.client())..cart.bindTo('op1');
    await tester.pumpWidget(_host(state, (_) async => null));
    await tester.tap(find.text('Scan Customer QR'));
    await tester.pumpAndSettle();
    expect(api.calls, isEmpty);
    expect(state.cart.customer, isNull);
  });
}
