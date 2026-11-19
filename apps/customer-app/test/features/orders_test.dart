import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_core/testing.dart';
import 'package:lutiano_customer/data/models.dart';
import 'package:lutiano_customer/features/orders/checkout_screen.dart';
import 'package:lutiano_customer/features/orders/order_detail_screen.dart';
import 'package:lutiano_customer/features/orders/orders_screen.dart';
import 'package:lutiano_customer/state/app_state.dart';
import 'package:lutiano_customer/state/cart_controller.dart';
import 'package:material_ui/material_ui.dart';

import '../support/fakes.dart';
import '../support/harness.dart';

Map<String, Object?> _order(String status) => {
      'id': 'o1',
      'reference': 'ORD-20261118-00001',
      'status': status,
      'total_xaf': 2300,
      'operation_id': 'op1',
      'operation_name': 'Bonanjo Cafe',
      'placed_at': '2026-11-18T10:00:00Z',
      'note': null,
      'reason': null,
      'sale_id': null,
      'lines': [
        {
          'product_name': 'Cappuccino',
          'quantity': 1,
          'unit_price_xaf': 2000,
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

AppState _stateWithCart(FakeApi api) {
  final state = AppState(api: api.client());
  state.selectSalesPoint(SalesPoint.fromJson(salesPointJson));
  final item = MenuItem.fromJson(cappuccino);
  state.cart.add(CartLine.of(item, size: item.sizes.last, addons: item.addons));
  return state;
}

void main() {
  testWidgets(
      'checkout sends the cart with its idempotency key, then empties it',
      (tester) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    String? key;
    Object? body;
    final api = FakeApi({
      'POST /customer/orders': (r) {
        key = r.headers['Idempotency-Key'];
        body = r.body;
        return (201, _order('PLACED'));
      },
    });
    final state = _stateWithCart(api);
    final cartKey = state.cart.checkoutKey;
    await tester
        .pumpWidget(testApp(home: const CheckoutScreen(), state: state));
    await tester.pumpAndSettle();
    expect(find.text('Cash at the sales point'), findsOneWidget);

    await tester.tap(find.text('Place Order'));
    await tester.pumpAndSettle();

    expect(key, cartKey);
    expect(body.toString(),
        allOf(contains('v2'), contains('a1'), contains('op1')));
    expect(find.text('Order Placed!'), findsOneWidget);
    expect(find.text('ORD-20261118-00001'), findsOneWidget);
    expect(state.cart.isEmpty, isTrue);
  });

  testWidgets('a refused order keeps the cart and shows why', (tester) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final api = FakeApi({
      'POST /customer/orders': (_) => (
            409,
            {
              'error': {'message': 'This sales point is closed right now.'}
            }
          ),
    });
    final state = _stateWithCart(api);
    await tester
        .pumpWidget(testApp(home: const CheckoutScreen(), state: state));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Place Order'));
    await tester.pumpAndSettle();
    expect(find.text('This sales point is closed right now.'), findsOneWidget);
    expect(state.cart.isEmpty, isFalse);
  });

  testWidgets('my orders lists orders with their status', (tester) async {
    final api = FakeApi({
      'GET /customer/orders': (_) => (200, FakeApi.page([_order('ACCEPTED')]))
    });
    await tester.pumpWidget(testApp(
        home: const OrdersScreen(), state: AppState(api: api.client())));
    await tester.pumpAndSettle();
    expect(find.text('ORD-20261118-00001'), findsOneWidget);
    expect(find.text('Being prepared'), findsOneWidget);
  });

  testWidgets('a new order can be cancelled', (tester) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    var cancelled = false;
    final api = FakeApi({
      'GET /customer/orders/o1': (_) =>
          (200, _order(cancelled ? 'CANCELLED' : 'PLACED')),
      'POST /customer/orders/o1/cancel': (_) {
        cancelled = true;
        return (200, _order('CANCELLED'));
      },
    });
    await tester.pumpWidget(testApp(
        home: const OrderDetailScreen(orderId: 'o1'),
        state: AppState(api: api.client())));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cancel Order'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cancel Order').last);
    await tester.pumpAndSettle();
    expect(cancelled, isTrue);
    expect(find.text('Cancel Order'), findsNothing);
  });
}
