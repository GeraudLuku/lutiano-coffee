import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/data/models.dart';
import 'package:lutiano_seller/features/requests/my_requests_screen.dart';
import 'package:lutiano_seller/features/requests/new_request_screen.dart';
import 'package:lutiano_seller/features/requests/request_details_screen.dart';
import 'package:lutiano_seller/features/requests/stock_requests_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';
import '../support/seller_fakes.dart';

AppState _state(FakeApi api, {String sellerRole = 'MOBILE_BARISTA'}) =>
    AppState(api: api.client())
      ..user = UserProfile(
          id: 'u1',
          email: 's@x',
          fullName: 'Sandra Eto',
          role: 'SELLER',
          sellerRole: sellerRole,
          operations: [operation])
      ..selectOperation(operation);

Widget _host(AppState state, Widget home) =>
    AppScope(state: state, child: testApp(home: home));

Map<String, Object?> _page(List<Object?> items,
        {Map<String, Object?> summary = const {}}) =>
    {
      'items': items,
      'page': 1,
      'size': 20,
      'total': items.length,
      'pages': 1,
      'summary': summary
    };

Map<String, Object?> _request(
        {String id = 'r1',
        String reference = 'SR-2026-00001',
        String status = 'PENDING_APPROVAL',
        String? note}) =>
    {
      'id': id,
      'reference': reference,
      'status': status,
      'operation_name': 'Bonapriso Cafe',
      'created_at': '2026-11-17T10:00:00Z',
      'item_count': 2,
      'total_quantity': 30,
      'reason': 'Weekly replenishment',
      'decision_note': note,
      'closed_at': '2026-11-17T12:00:00Z',
      'items': [
        {'product_id': 'p1', 'product_name': 'Latte', 'sent_quantity': 20},
        {'product_id': 'p2', 'product_name': 'Mocha', 'sent_quantity': 10},
      ],
    };

Map<String, Object?> _stock(String id, String name) => {
      'product_id': id,
      'sku': id,
      'name': name,
      'category': 'Drinks',
      'price_xaf': 2000,
      'low_stock_threshold': 5,
      'quantity': 12,
    };

void main() {
  group('overview', () {
    final routes = {
      'GET /inventory/requests/mine': (_) => (
            200,
            _page([], summary: {
              'total': 12,
              'pending': 2,
              'approved': 7,
              'in_fulfillment': 2,
              'completed': 0,
              'rejected': 1
            })
          ),
    };

    testWidgets('shows the counts and the Request Stock button',
        (tester) async {
      final api = FakeApi(routes);
      await tester.pumpWidget(_host(_state(api), const StockRequestsScreen()));
      await tester.pumpAndSettle();
      expect(find.text('12'), findsOneWidget);
      expect(find.text('7'), findsOneWidget);
      expect(find.text('Request Stock'), findsOneWidget);
      expect(api.uris.single.queryParameters['size'], '1',
          reason: 'only the counts are needed here');
    });

    testWidgets('a fixed point of sale sees the status but cannot request',
        (tester) async {
      await tester.pumpWidget(_host(
          _state(FakeApi(routes), sellerRole: 'FIXED_POS_BARISTA'),
          const StockRequestsScreen()));
      await tester.pumpAndSettle();
      expect(find.text('12'), findsOneWidget);
      expect(find.text('Request Stock'), findsNothing);
    });
  });

  testWidgets(
      'choose products, review, submit: one request with every product and a key; a failed send is retried with the same key',
      (tester) async {
    final bodies = <Map<String, dynamic>>[];
    var fail = true;
    final api = FakeApi({
      'GET /inventory': (_) => (
            200,
            _page([
              _stock('p1', 'Latte'),
              _stock('p2', 'Mocha')
            ], summary: {
              'categories': ['Drinks']
            })
          ),
      'GET /inventory/requests/mine': (_) => (200, _page([])),
      'POST /inventory/requests': (r) {
        bodies.add(jsonDecode(r.body) as Map<String, dynamic>);
        if (fail) {
          fail = false;
          return (
            409,
            {
              'error': {'message': 'Not enough central stock: Mocha.'}
            }
          );
        }
        return (201, _request());
      },
    });
    final state = _state(api);
    StockRequest? created;
    await tester.pumpWidget(_host(
        state,
        Builder(
            builder: (context) => TextButton(
                onPressed: () async => created = await Navigator.of(context)
                    .push<StockRequest>(MaterialPageRoute(
                        builder: (_) => const NewRequestScreen())),
                child: const Text('go')))));
    await tester.tap(find.text('go'));
    await tester.pumpAndSettle();

    expect(find.text('0 products selected'), findsOneWidget);
    await tester.tap(find.byType(Checkbox).first);
    await tester.pump();
    await tester.tap(find.byIcon(Icons.add).first); // Latte: 1 -> 2
    await tester.pump();
    await tester.tap(find.byType(Checkbox).last);
    await tester.pump();
    expect(find.text('2 products selected'), findsOneWidget);
    expect(find.text('Total quantity: 3'), findsOneWidget);
    await tester.tap(find.text('Next'));
    await tester.pumpAndSettle();

    expect(find.text('Request Summary'), findsOneWidget);
    expect(find.text('As soon as possible'), findsOneWidget);
    await tester.tap(find.text('Submit Request'));
    await tester.pumpAndSettle();
    expect(find.text('Not enough central stock: Mocha.'), findsOneWidget,
        reason: 'the server\'s message, and the form stays');
    await tester.tap(find.text('Submit Request'));
    await tester.pumpAndSettle();

    expect(bodies.length, 2);
    expect(bodies[0]['idempotencyKey'], bodies[1]['idempotencyKey'],
        reason: 'a retry must not create a second request');
    expect(bodies[1]['operationId'], 'op1');
    expect(bodies[1]['items'], [
      {'productId': 'p1', 'quantity': 2},
      {'productId': 'p2', 'quantity': 1},
    ]);
    expect(bodies[1].containsKey('neededBy'), false);
    expect(created?.reference, 'SR-2026-00001');
  });

  testWidgets('My Requests filters by status and searches by reference',
      (tester) async {
    final api = FakeApi({
      'GET /inventory/requests/mine': (_) => (200, _page([_request()])),
    });
    await tester.pumpWidget(_host(_state(api), const MyRequestsScreen()));
    await tester.pumpAndSettle();
    expect(find.text('SR-2026-00001'), findsOneWidget);
    expect(api.uris.last.queryParameters.containsKey('status'), false);

    await tester.ensureVisible(find.text('Rejected'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Rejected'));
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['status'], 'REJECTED');

    await tester.enterText(find.byType(TextField), 'sr-2026');
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['reference'], 'sr-2026');
    expect(api.uris.last.queryParameters['status'], 'REJECTED');
  });

  group('details', () {
    Future<void> open(WidgetTester tester, Map<String, Object?> request,
        {String sellerRole = 'MOBILE_BARISTA'}) async {
      final api = FakeApi({
        'GET /inventory/requests/r1': (_) => (200, request),
      });
      await tester.pumpWidget(_host(_state(api, sellerRole: sellerRole),
          const RequestDetailsScreen(requestId: 'r1')));
      await tester.pumpAndSettle();
    }

    testWidgets('a pending request waits for review and cannot be duplicated',
        (tester) async {
      await open(tester, _request());
      expect(find.text('SR-2026-00001'), findsOneWidget);
      expect(find.text('Pending'), findsOneWidget);
      expect(find.text('Latte'), findsOneWidget);
      expect(find.text('Total quantity'), findsOneWidget);
      expect(find.textContaining('awaiting review'), findsOneWidget);
      expect(find.text('Duplicate & Modify'), findsNothing);
    });

    testWidgets('a rejected request shows why, and can be duplicated',
        (tester) async {
      await open(
          tester,
          _request(
              status: 'REJECTED',
              note: 'Quantity exceeds allowed limit for this operation.'));
      expect(find.text('Rejected'), findsOneWidget);
      expect(find.textContaining('Quantity exceeds allowed limit'),
          findsOneWidget);
      await tester.scrollUntilVisible(find.text('Duplicate & Modify'), 200);
      expect(find.text('Duplicate & Modify'), findsOneWidget);
    });

    testWidgets('a fixed point of sale cannot duplicate', (tester) async {
      await open(tester, _request(status: 'REJECTED'),
          sellerRole: 'FIXED_POS_BARISTA');
      expect(find.text('Duplicate & Modify'), findsNothing);
    });

    testWidgets('a dispatched request shows the progress and what to do',
        (tester) async {
      await open(tester, {
        ..._request(status: 'IN_TRANSIT'),
        'approved_at': '2026-11-17T11:00:00Z',
        'dispatched_at': '2026-11-17T13:00:00Z',
      });
      expect(find.text('In fulfillment'), findsOneWidget);
      expect(find.text('Fulfillment progress'), findsOneWidget);
      expect(find.textContaining('Confirm each delivery'), findsOneWidget);
    });
  });
}
