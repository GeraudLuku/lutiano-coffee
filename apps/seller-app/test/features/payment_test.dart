import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:lutiano_seller/data/models.dart';
import 'package:lutiano_seller/features/sale/payment_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';
import '../support/seller_fakes.dart';

Widget _host(AppState state) =>
    AppScope(state: state, child: testApp(home: PaymentScreen()));

AppState _state(FakeApi api) {
  final state = AppState(api: api.client());
  state.selectOperation(operation);
  state.cart.add(latte, available: 10);
  state.cart.add(latte, available: 10);
  return state;
}

/// A tall screen, so the whole payment form is built (a ListView builds only what is visible).
void _tallScreen(WidgetTester tester) {
  tester.view.physicalSize = const Size(800, 2400);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
}

void main() {
  testWidgets(
      'records the sale with the customer token, clears the cart and shows the confirmation',
      (tester) async {
    _tallScreen(tester);
    late http.Request posted;
    final api = FakeApi({
      'GET /payment-methods': (_) => (200, cashMethods),
      'POST /sales': (r) {
        posted = r;
        return (201, saleJson(customer: 'Mireille Bessala', points: 40));
      },
      'GET /shifts/open': (_) => (200, [openShiftJson]),
    });
    final state = _state(api);
    state.cart.attachCustomer(
        const ScannedCustomer(
            fullName: 'Mireille Bessala', code: 'CU-1', pointsBalance: 10),
        'tok');
    await tester.pumpWidget(_host(state));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Confirm Payment'));
    await tester.pumpAndSettle();

    final body = jsonDecode(posted.body) as Map<String, dynamic>;
    expect(body['customerToken'], 'tok');
    expect(body['lines'], [
      {'productId': 'p1', 'quantity': 2}
    ]);
    expect(posted.headers['Idempotency-Key'], isNotEmpty);
    expect(state.cart.isEmpty, true);
    expect(find.text('Sale Completed!'), findsOneWidget);
    expect(find.text('Mireille Bessala'), findsOneWidget);
    expect(find.text('+40 pts'), findsOneWidget);
  });

  testWidgets(
      'a lost connection keeps the cart and retries with the same idempotency key',
      (tester) async {
    _tallScreen(tester);
    final keys = <String?>[];
    var attempts = 0;
    final api = FakeApi({
      'GET /payment-methods': (_) => (200, cashMethods),
      'POST /sales': (r) {
        keys.add(r.headers['Idempotency-Key']);
        if (++attempts == 1) throw http.ClientException('offline');
        return (201, saleJson());
      },
      'GET /shifts/open': (_) => (200, [openShiftJson]),
    });
    final state = _state(api);
    await tester.pumpWidget(_host(state));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Confirm Payment'));
    await tester.pumpAndSettle();
    expect(
        find.textContaining('Your cart is saved as a draft'), findsOneWidget);
    expect(state.cart.isEmpty, false);

    await tester.tap(find.text('Confirm Payment'));
    await tester.pumpAndSettle();
    expect(keys.length, 2);
    expect(keys[0], keys[1]);
    expect(find.text('Sale Completed!'), findsOneWidget);
  });

  testWidgets('a server refusal shows its message and keeps the cart',
      (tester) async {
    _tallScreen(tester);
    final api = FakeApi({
      'GET /payment-methods': (_) => (200, cashMethods),
      'POST /sales': (_) => (
            409,
            {
              'error': {
                'message': 'Not enough stock of Latte at this operation.'
              }
            }
          ),
    });
    final state = _state(api);
    await tester.pumpWidget(_host(state));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Confirm Payment'));
    await tester.pumpAndSettle();
    expect(find.text('Not enough stock of Latte at this operation.'),
        findsOneWidget);
    expect(state.cart.isEmpty, false);
  });

  testWidgets('cash below the total is refused before anything is sent',
      (tester) async {
    _tallScreen(tester);
    final api = FakeApi({'GET /payment-methods': (_) => (200, cashMethods)});
    final state = _state(api);
    await tester.pumpWidget(_host(state));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '1000');
    await tester.tap(find.text('Confirm Payment'));
    await tester.pumpAndSettle();
    expect(
        find.textContaining('Cash received must be at least'), findsOneWidget);
    expect(api.calls, ['GET /payment-methods']);
  });
}
