import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/features/inventory/stock_list_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';
import 'package:lutiano_seller/widgets/widgets.dart';
import 'package:material_ui/material_ui.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';
import '../support/seller_fakes.dart';

Map<String, Object?> _row(String name, String category) => {
      'product_id': name,
      'sku': name,
      'name': name,
      'category': category,
      'price_xaf': 2000,
      'low_stock_threshold': 5,
      'quantity': 10,
    };

Widget _host(AppState state) =>
    AppScope(state: state, child: testApp(home: const StockListScreen()));

void main() {
  testWidgets(
      'search and category are sent to the server (after a pause), not filtered on the phone',
      (tester) async {
    final api = FakeApi({
      'GET /inventory': (r) {
        final q = r.url.queryParameters['q'];
        final category = r.url.queryParameters['category'];
        final rows = [
          _row('Latte', 'Drinks'),
          _row('Croissant', 'Pastry'),
        ].where((p) =>
            (q == null || '${p['name']}'.toLowerCase().contains(q)) &&
            (category == null || p['category'] == category));
        return (
          200,
          {
            'items': rows.toList(),
            'page': 1,
            'size': 20,
            'total': rows.length,
            'pages': 1,
            'summary': {
              'categories': ['Drinks', 'Pastry']
            }
          }
        );
      },
    });
    final state = AppState(api: api.client())..selectOperation(operation);
    await tester.pumpWidget(_host(state));
    await tester.pumpAndSettle();
    expect(find.text('Latte'), findsOneWidget);
    expect(find.text('Croissant'), findsOneWidget);
    expect(api.uris.single.queryParameters.containsKey('q'), false);

    await tester.enterText(find.byType(TextField), 'lat');
    await tester.pump(const Duration(milliseconds: 100));
    expect(api.uris.length, 1, reason: 'nothing is sent while still typing');
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['q'], 'lat');
    expect(find.text('Latte'), findsOneWidget);
    expect(find.text('Croissant'), findsNothing);

    await tester.enterText(find.byType(TextField), '');
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Pastry').first);
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['category'], 'Pastry');
    expect(find.text('Croissant'), findsOneWidget);
    expect(find.text('Latte'), findsNothing);
  });

  testWidgets('shows skeleton rows until the first page arrives',
      (tester) async {
    final api = FakeApi({
      'GET /inventory': (_) => (
            200,
            {
              'items': [_row('Latte', 'Drinks')],
              'page': 1,
              'size': 20,
              'total': 1,
              'pages': 1,
              'summary': {'categories': <String>[]}
            }
          ),
    });
    final state = AppState(api: api.client())..selectOperation(operation);
    await tester.pumpWidget(_host(state));
    expect(find.byType(SkeletonCard), findsWidgets,
        reason: 'the answer has not been read yet');
    await tester.pumpAndSettle();
    expect(find.byType(SkeletonCard), findsNothing);
    expect(find.text('Latte'), findsOneWidget);
  });
}
