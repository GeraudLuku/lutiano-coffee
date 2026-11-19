import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:lutiano_seller/data/models.dart';
import 'package:lutiano_seller/features/inventory/inventory_summary_screen.dart';
import 'package:lutiano_seller/features/inventory/stock_detail_screen.dart';
import 'package:lutiano_seller/features/inventory/stock_movements_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';
import '../support/seller_fakes.dart';

Map<String, Object?> _movement({
  required String direction,
  String type = 'SALE',
  int quantity = 2,
}) =>
    {
      'direction': direction,
      'product_id': 'p1',
      'product_name': 'Latte',
      'quantity': quantity,
      'balance_after': 18,
      'occurred_at': '2026-11-17T10:00:00Z',
      'movement_type': type,
      'source_id': direction == 'OUT' ? 's1' : 'a1',
      'reference': 'SL-1',
      'source': 'Sale',
    };

Map<String, Object?> _page(List<Object?> items) => {
      'items': items,
      'page': 1,
      'size': 20,
      'total': items.length,
      'pages': 1,
      'summary': {}
    };

Widget _host(AppState state, Widget home) =>
    AppScope(state: state, child: testApp(home: home));

void main() {
  testWidgets(
      'movements are asked for one period at a time, and a sale opens its consumption and then the sale',
      (tester) async {
    final api = FakeApi({
      'GET /inventory/movements': (_) => (
            200,
            _page([
              _movement(direction: 'OUT'),
              _movement(direction: 'IN', type: 'TRANSFER_IN', quantity: 50),
            ])
          ),
      'GET /sales/s1': (_) => (200, saleJson()),
    });
    final state = AppState(api: api.client())..selectOperation(operation);
    await tester.pumpWidget(_host(state, const StockMovementsScreen()));
    await tester.pumpAndSettle();
    final first = api.uris.first.queryParameters;
    expect(first['from'], matches(RegExp(r'^\d{4}-\d{2}-01$')),
        reason: 'the month starts on the 1st');
    expect(first['to'], isNotNull);

    await tester.tap(find.text('Today'));
    await tester.pumpAndSettle();
    final today = api.uris.last.queryParameters;
    expect(today['from'], today['to']);

    // an incoming allocation is not tappable; the sale's consumption is
    await tester.tap(find.text('Stock In · Latte'));
    await tester.pumpAndSettle();
    expect(find.text('Consumption detail'), findsNothing);
    await tester.tap(find.text('Stock Out · Latte'));
    await tester.pumpAndSettle();
    expect(find.text('Consumption detail'), findsOneWidget);
    expect(find.text('Remaining stock'), findsOneWidget);
    expect(find.text('18'), findsOneWidget,
        reason: 'the balance after the sale');
    await tester.tap(find.text('View sale details'));
    await tester.pumpAndSettle();
    expect(api.calls, contains('GET /sales/s1'));
  });

  testWidgets('the summary shows the period totals and the top consumed',
      (tester) async {
    final api = FakeApi({
      'GET /inventory/summary': (_) => (
            200,
            {
              'received': 120,
              'consumed': 98,
              'net': 22,
              'top_consumed': [
                {'product_id': 'p1', 'product_name': 'Latte', 'quantity': 28},
                {'product_id': 'p2', 'product_name': 'Mocha', 'quantity': 24},
              ]
            }
          ),
    });
    final state = AppState(api: api.client())..selectOperation(operation);
    await tester.pumpWidget(_host(state, const InventorySummaryScreen()));
    await tester.pumpAndSettle();
    expect(find.text('120'), findsOneWidget);
    expect(find.text('98'), findsOneWidget);
    expect(find.text('+22'), findsOneWidget);
    expect(find.text('Latte'), findsOneWidget);
    expect(find.text('28'), findsOneWidget);
    expect(api.uris.single.queryParameters['operationId'], 'op1');
  });

  testWidgets(
      'product stock shows received and consumed today, with no made-up level bar',
      (tester) async {
    final api = FakeApi({
      'GET /inventory/summary': (r) {
        final today = r.url.queryParameters.containsKey('from');
        return (
          200,
          {
            'received': today ? 0 : 80,
            'consumed': today ? 3 : 62,
            'net': 0,
            'top_consumed': []
          }
        );
      },
      'GET /inventory/movements': (_) => (200, _page([])),
    });
    final state = AppState(api: api.client())..selectOperation(operation);
    await tester.pumpWidget(_host(state,
        const StockDetailScreen(item: StockItem(product: latte, quantity: 4))));
    await tester.pumpAndSettle();
    expect(find.text('Total received'), findsOneWidget);
    expect(find.text('80'), findsOneWidget);
    expect(find.text('Consumed today'), findsOneWidget);
    expect(find.text('3'), findsOneWidget);
    expect(find.text('Stock Level'), findsNothing);
    expect(find.text('Minimum Level'), findsOneWidget);
    final sent = [
      for (final u in api.uris.where((u) => u.path.endsWith('summary')))
        u.queryParameters
    ];
    expect(sent.any((q) => q['productId'] == 'p1' && !q.containsKey('from')),
        true);
    expect(
        sent.any((q) => q['productId'] == 'p1' && q['from'] == q['to']), true);
  });
}
