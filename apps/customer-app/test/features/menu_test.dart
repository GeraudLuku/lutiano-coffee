import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:lutiano_core/testing.dart';
import 'package:material_ui/material_ui.dart';
import 'package:lutiano_customer/data/models.dart';
import 'package:lutiano_customer/features/menu/menu_screen.dart';
import 'package:lutiano_customer/features/menu/product_screen.dart';
import 'package:lutiano_customer/state/app_state.dart';

import '../support/fakes.dart';
import '../support/harness.dart';

AppState _state(FakeApi api) {
  final state = AppState(api: api.client());
  state.selectSalesPoint(SalesPoint.fromJson(salesPointJson));
  return state;
}

void main() {
  testWidgets(
      'the menu at a sales point lists products and marks the unavailable ones',
      (tester) async {
    final api = FakeApi({
      'GET /public/categories': (_) => (
            200,
            FakeApi.page([
              {'id': 'c1', 'name': 'Beverage'}
            ])
          ),
      'GET /public/operations/op1/products': (_) => (
            200,
            FakeApi.page([
              cappuccino,
              {...croissant, 'available': false}
            ])
          ),
    });
    await tester
        .pumpWidget(testApp(home: const MenuScreen(), state: _state(api)));
    await tester.pumpAndSettle();

    expect(find.text('Ordering from Bonanjo Cafe'), findsOneWidget);
    expect(find.text('Cappuccino'), findsOneWidget);
    expect(find.text('from ${xaf(1200)}'), findsOneWidget);
    expect(find.text('Not available at this sales point right now.'),
        findsOneWidget);
  });

  testWidgets(
      'choosing a size and an add-on prices the line and adds it to the cart',
      (tester) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final api = FakeApi({});
    final state = _state(api);
    await tester.pumpWidget(testApp(
        home: ProductScreen(item: MenuItem.fromJson(cappuccino)),
        state: state));
    await tester.pumpAndSettle();

    expect(find.text('Add to Cart (${xaf(1200)})'), findsOneWidget);
    await tester.tap(find.textContaining('Large'));
    await tester.pump();
    await tester.tap(find.text('Caramel Syrup'));
    await tester.pump();
    expect(find.text('Add to Cart (${xaf(2300)})'), findsOneWidget);

    await tester.tap(find.text('Add to Cart (${xaf(2300)})'));
    await tester.pumpAndSettle();
    expect(find.text('Added to cart!'), findsOneWidget);
    expect(state.cart.count, 1);
    expect(state.cart.totalXaf, 2300);
    expect(state.cart.lines.single.productId, 'v2',
        reason: 'the size is what is sold');
  });
}
