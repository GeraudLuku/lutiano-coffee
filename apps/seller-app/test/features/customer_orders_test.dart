import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:lutiano_seller/features/orders/customer_order_screen.dart';
import 'package:lutiano_seller/features/orders/customer_orders_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';

Map<String, Object?> _order(String status) => {
      'id': 'o1',
      'reference': 'ORD-20261118-00001',
      'status': status,
      'total_xaf': 4200,
      'customer_name': 'Ada Lovelace',
      'operation_name': 'Bonapriso Cafe',
      'placed_at': '2026-11-18T10:00:00Z',
      'note': 'Less sugar',
      'lines': [
        {
          'product_name': 'Americano',
          'quantity': 2,
          'unit_price_xaf': 1800,
          'addons_unit_xaf': 300,
          'addons': [
            {'addon_name': 'Caramel Syrup'}
          ]
        }
      ],
      'timeline': [
        {'status': 'PLACED', 'at': '2026-11-18T10:00:00Z'}
      ],
    };

void main() {
  testWidgets('the list shows orders waiting for the seller', (tester) async {
    final api = FakeApi({
      'GET /seller/orders': (_) => (200, FakeApi.page([_order('PLACED')])),
    });
    await tester.pumpWidget(AppScope(
        state: AppState(api: api.client()),
        child: testApp(home: const CustomerOrdersScreen())));
    await tester.pumpAndSettle();
    expect(find.text('ORD-20261118-00001'), findsOneWidget);
    expect(find.text('New'), findsOneWidget);
    expect(api.uris.first.queryParameters['group'], 'PENDING');
  });

  testWidgets('a new order is accepted, then marked ready', (tester) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final calls = <String>[];
    final api = FakeApi({
      'GET /seller/orders/o1': (_) => (200, _order('PLACED')),
      'POST /seller/orders/o1/accept': (_) {
        calls.add('accept');
        return (200, _order('ACCEPTED'));
      },
      'POST /seller/orders/o1/ready': (_) {
        calls.add('ready');
        return (200, _order('READY'));
      },
    });
    await tester.pumpWidget(AppScope(
        state: AppState(api: api.client()),
        child: testApp(home: const CustomerOrderScreen(orderId: 'o1'))));
    await tester.pumpAndSettle();
    expect(find.text('Caramel Syrup', skipOffstage: false), findsNothing);
    expect(find.textContaining('Caramel Syrup'), findsOneWidget);
    expect(find.textContaining('Less sugar'), findsOneWidget);

    await tester.tap(find.text('Accept Order'));
    await tester.pumpAndSettle();
    expect(find.text('Mark as Ready'), findsOneWidget);
    await tester.tap(find.text('Mark as Ready'));
    await tester.pumpAndSettle();
    expect(calls, ['accept', 'ready']);
    expect(find.text('Hand Over'), findsOneWidget);
  });
}
