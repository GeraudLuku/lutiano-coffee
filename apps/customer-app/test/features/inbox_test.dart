import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_core/testing.dart';
import 'package:lutiano_customer/features/notifications/notification_preferences_screen.dart';
import 'package:lutiano_customer/features/notifications/notifications_screen.dart';
import 'package:lutiano_customer/features/support/support_request_screen.dart';
import 'package:lutiano_customer/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

import '../support/harness.dart';

Map<String, Object?> _note(String id, {bool read = false}) => {
      'id': id,
      'event_key': 'ORDER_READY',
      'category': 'ORDERS',
      'title': null,
      'body': 'your order ORD-1 is ready to collect.',
      'important': false,
      'related_type': 'customer_order',
      'related_id': 'o1',
      'created_at': '2026-11-18T10:00:00Z',
      'read_at': read ? '2026-11-18T10:05:00Z' : null,
    };

void main() {
  testWidgets('the inbox lists messages and marks one read when opened',
      (tester) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final read = <String>[];
    final api = FakeApi({
      'GET /customer/notifications': (_) => (200, FakeApi.page([_note('n1')])),
      'POST /customer/notifications/read': (r) {
        read.add(r.body);
        return (200, {'changed': 1});
      },
      'GET /customer/notifications/unread-count': (_) => (200, {'unread': 0}),
      'GET /customer/orders/o1': (_) => (
            404,
            {
              'error': {'message': 'x'}
            }
          ),
    });
    await tester.pumpWidget(testApp(
        home: const NotificationsScreen(), state: AppState(api: api.client())));
    await tester.pumpAndSettle();
    expect(find.text('Order ready'), findsOneWidget);
    await tester.tap(find.text('Order ready'));
    await tester.pumpAndSettle();
    expect(read.single, contains('n1'));
  });

  testWidgets('order updates cannot be switched off, offers can',
      (tester) async {
    final api = FakeApi({
      'GET /customer/notifications/preferences': (_) => (
            200,
            [
              {
                'category': 'ORDERS',
                'in_app': true,
                'push': true,
                'locked': true
              },
              {
                'category': 'PROMOTIONS',
                'in_app': true,
                'push': true,
                'locked': false
              },
            ]
          ),
      'PUT /customer/notifications/preferences/PROMOTIONS': (r) => (
            200,
            [
              {
                'category': 'ORDERS',
                'in_app': true,
                'push': true,
                'locked': true
              },
              {
                'category': 'PROMOTIONS',
                'in_app': false,
                'push': true,
                'locked': false
              },
            ]
          ),
    });
    await tester.pumpWidget(testApp(
        home: const NotificationPreferencesScreen(),
        state: AppState(api: api.client())));
    await tester.pumpAndSettle();
    expect(find.text('Always on'), findsOneWidget);
    final switches = tester.widgetList<Switch>(find.byType(Switch)).toList();
    expect(switches.first.onChanged, isNull, reason: 'Orders is locked');
    await tester.tap(find.byType(Switch).last);
    await tester.pumpAndSettle();
    expect(api.calls,
        contains('PUT /customer/notifications/preferences/PROMOTIONS'));
  });

  testWidgets('a support request needs a title and details, then is sent',
      (tester) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final api = FakeApi({
      'POST /customer/support': (_) => (
            201,
            {
              'id': 'q1',
              'reference': 'SP-2026-00001',
              'type': 'ORDER',
              'subject': 'Late order',
              'description': 'It is late',
              'status': 'OPEN',
              'created_at': '2026-11-18T10:00:00Z'
            }
          ),
      'GET /customer/support/q1': (_) => (
            200,
            {
              'id': 'q1',
              'reference': 'SP-2026-00001',
              'type': 'ORDER',
              'subject': 'Late order',
              'description': 'It is late',
              'status': 'OPEN',
              'created_at': '2026-11-18T10:00:00Z'
            }
          ),
    });
    await tester.pumpWidget(testApp(
        home: const SupportRequestScreen(),
        state: AppState(api: api.client())));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Send Request'));
    await tester.pumpAndSettle();
    expect(find.text('Enter a short title.'), findsOneWidget);
    expect(api.calls, isEmpty);

    await tester.enterText(find.byType(TextFormField).at(0), 'Late order');
    await tester.enterText(find.byType(TextFormField).at(1), 'It is late');
    await tester.tap(find.text('Send Request'));
    await tester.pumpAndSettle();
    expect(api.calls.first, 'POST /customer/support');
    expect(find.text('SP-2026-00001'), findsWidgets);
    expect(find.text('The team has not replied yet. You will be notified.'),
        findsOneWidget);
  });
}
