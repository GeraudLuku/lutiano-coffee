import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:material_ui/material_ui.dart';
import 'package:lutiano_seller/features/inventory/incoming_stock_screen.dart';
import 'package:lutiano_seller/features/shift/shift_dialogs.dart';
import 'package:lutiano_seller/data/models.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';
import '../support/seller_fakes.dart';

Widget _host(AppState state, Widget body) =>
    AppScope(state: state, child: testApp(home: Scaffold(body: body)));

Widget _button(
        String label, Future<void> Function(BuildContext) action) =>
    Builder(
        builder: (context) =>
            TextButton(onPressed: () => action(context), child: Text(label)));

const _allocation = {
  'id': 'a1',
  'operation_id': 'op1',
  'operation_name': 'Bonapriso Cafe',
  'product_name': 'Latte',
  'sent_quantity': 20,
  'created_at': '2026-11-17T08:00:00Z'
};

void main() {
  group('open shift', () {
    testWidgets('opens the shift with the float and refreshes it',
        (tester) async {
      var open = false;
      final api = FakeApi({
        'POST /shifts/open': (_) {
          open = true;
          return (200, {});
        },
        'GET /shifts/open': (_) => (200, open ? [openShiftJson] : []),
      });
      final state = AppState(api: api.client());
      bool? opened;
      await tester.pumpWidget(_host(state,
          _button('go', (c) async => opened = await showOpenShiftDialog(c))));
      await tester.tap(find.text('go'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField), '10000');
      await tester.tap(find.text('Open shift'));
      await tester.pumpAndSettle();
      expect(opened, true);
      expect(state.shift?.openingFloat, 10000);
    });

    testWidgets('a refusal stays in the dialog with the server message',
        (tester) async {
      final api = FakeApi({
        'POST /shifts/open': (_) => (
              409,
              {
                'error': {'message': 'You already have an open shift.'}
              }
            )
      });
      await tester.pumpWidget(_host(AppState(api: api.client()),
          _button('go', (c) async => showOpenShiftDialog(c))));
      await tester.tap(find.text('go'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Open shift'));
      await tester.pumpAndSettle();
      expect(find.text('You already have an open shift.'), findsOneWidget);
    });
  });

  group('close shift', () {
    AppState stateWithShift(FakeApi api) =>
        AppState(api: api.client())..shift = CashShift.fromJson(openShiftJson);

    testWidgets('a difference needs an explanation before anything is sent',
        (tester) async {
      final api = FakeApi({});
      await tester.pumpWidget(
          _host(stateWithShift(api), _button('go', showCloseShiftDialog)));
      await tester.tap(find.text('go'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField).first, '13000');
      await tester.tap(find.text('Close shift'));
      await tester.pumpAndSettle();
      expect(
          find.text('Explain the difference before closing.'), findsOneWidget);
      expect(api.calls, isNot(contains('POST /shifts/close')),
          reason: 'nothing is sent until the difference is explained');
    });

    testWidgets('matching cash closes the shift and says so', (tester) async {
      final api = FakeApi({
        'POST /shifts/close': (_) =>
            (200, {'expectedCash': 14000, 'countedCash': 14000, 'variance': 0}),
        'GET /shifts/open': (_) => (200, []),
      });
      await tester.pumpWidget(
          _host(stateWithShift(api), _button('go', showCloseShiftDialog)));
      await tester.tap(find.text('go'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField).first, '14000');
      await tester.tap(find.text('Close shift'));
      await tester.pumpAndSettle();
      expect(find.text('Shift closed. Cash matches.'), findsOneWidget);
    });
  });

  group('receive stock', () {
    FakeApi api(Map<String, (int, Object?) Function(http.Request)> extra) =>
        FakeApi({
          'GET /inventory/allocations/mine': (_) =>
              (200, FakeApi.page([_allocation])),
          ...extra,
        });

    Future<void> openConfirm(WidgetTester tester, FakeApi fake) async {
      final state = AppState(api: fake.client())..selectOperation(operation);
      await tester.pumpWidget(
          AppScope(state: state, child: testApp(home: IncomingStockScreen())));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Confirm Receipt'));
      await tester.pumpAndSettle();
    }

    testWidgets('a full delivery is confirmed as received', (tester) async {
      final fake = api({
        'POST /inventory/allocations/confirm': (_) =>
            (200, {'discrepancy': false})
      });
      await openConfirm(tester, fake);
      await tester.tap(find.text('Confirm'));
      await tester.pumpAndSettle();
      expect(find.text('Receipt confirmed. 20 units added to your stock.'),
          findsOneWidget);
    });

    testWidgets(
        'a different quantity needs a reason, then is reported as a difference',
        (tester) async {
      final fake = api({
        'POST /inventory/allocations/confirm': (_) =>
            (200, {'discrepancy': true})
      });
      await openConfirm(tester, fake);
      await tester.enterText(find.widgetWithText(TextField, '20'), '18');
      await tester.pumpAndSettle();
      await tester.tap(find.text('Confirm'));
      await tester.pumpAndSettle();
      expect(
          find.text('Explain why the quantity is different.'), findsOneWidget);
      expect(fake.calls.where((c) => c.startsWith('POST')), isEmpty);

      await tester.enterText(find.widgetWithText(TextField, ''), 'two damaged');
      await tester.tap(find.text('Confirm'));
      await tester.pumpAndSettle();
      expect(find.text('Receipt confirmed. The difference has been reported.'),
          findsOneWidget);
    });

    testWidgets('more than was sent is refused', (tester) async {
      final fake = api({});
      await openConfirm(tester, fake);
      await tester.enterText(find.widgetWithText(TextField, '20'), '25');
      await tester.tap(find.text('Confirm'));
      await tester.pumpAndSettle();
      expect(find.text('Enter a quantity between 0 and 20.'), findsOneWidget);
    });
  });
}
