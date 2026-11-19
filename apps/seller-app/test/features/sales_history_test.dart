import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/features/history/sales_history_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';
import 'package:lutiano_seller/widgets/widgets.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';
import '../support/seller_fakes.dart';

void main() {
  testWidgets(
      'the totals come from the server for the whole period, not from the rows on screen',
      (tester) async {
    final api = FakeApi({
      'GET /sales': (r) => (
            200,
            {
              // one row on screen, but the period holds 37 sales
              'items': [saleJson(reference: 'SL-1')],
              'page': 1,
              'size': 20,
              'total': 37,
              'pages': 1,
              'summary': {'sales': 37, 'revenue_xaf': 250000, 'units': 91}
            }
          ),
    });
    final state = AppState(api: api.client())..selectOperation(operation);
    await tester.pumpWidget(AppScope(
        state: state, child: testApp(home: const SalesHistoryScreen())));
    expect(find.byType(SkeletonMetrics), findsOneWidget,
        reason: 'totals show a skeleton until the first page arrives');
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pump(const Duration(milliseconds: 100));
    expect(find.text('37'), findsOneWidget);
    expect(find.text('250,000'), findsOneWidget);
    expect(find.text('91'), findsOneWidget);
    expect(find.text('SL-1'), findsOneWidget);
    expect(api.uris.first.queryParameters['page'], '1');
  });
}
