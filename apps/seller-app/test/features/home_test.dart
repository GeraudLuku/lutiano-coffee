import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/app.dart';
import 'package:lutiano_seller/data/models.dart';
import 'package:lutiano_seller/state/app_state.dart';

import '../support/fake_api.dart';
import '../support/seller_fakes.dart';

void main() {
  testWidgets(
      "a failing summary shows its own error and retry; the rest of Home still works",
      (tester) async {
    var summaryCalls = 0;
    final api = FakeApi({
      'GET /me/summary': (_) {
        summaryCalls++;
        if (summaryCalls == 1) return (500, {});
        const day = {
          'orders': 3,
          'sales_xaf': 12500,
          'customers': 1,
          'points_issued': 40,
          'commission_xaf': 625
        };
        return (200, {'today': day, 'month': day});
      },
      'GET /operations': (_) => (200, FakeApi.page([operationJson])),
      'GET /shifts/open': (_) => (200, []),
      'GET /sales': (_) => (200, FakeApi.page([])),
      'GET /inventory/allocations/mine': (_) => (200, FakeApi.page([])),
    });
    final state = AppState(api: api.client())
      ..user = UserProfile(
          id: 'u1',
          email: 's@x',
          fullName: 'Martin Nguemaleu',
          role: 'SELLER',
          operations: [operation])
      ..selectOperation(operation);
    await tester.pumpWidget(SellerApp(state: state));
    await tester.pumpAndSettle();

    expect(find.text('New Sale'), findsOneWidget,
        reason: 'one failing card must not take Home down');
    expect(find.textContaining('Something went wrong'), findsOneWidget);
    await tester.tap(find.text('Retry'));
    await tester.pumpAndSettle();
    expect(find.text('12,500 XAF'), findsOneWidget);
    expect(find.text('40 pts'), findsOneWidget);
  });
}
