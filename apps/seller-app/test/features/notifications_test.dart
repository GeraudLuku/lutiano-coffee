import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/features/home/home_widgets.dart';
import 'package:lutiano_seller/features/notifications/notification_categories_screen.dart';
import 'package:lutiano_seller/features/notifications/notification_details_screen.dart';
import 'package:lutiano_seller/features/notifications/notification_history_screen.dart';
import 'package:lutiano_seller/features/notifications/notification_preferences_screen.dart';
import 'package:lutiano_seller/features/notifications/notifications_screen.dart';
import 'package:lutiano_seller/data/models.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';
import '../support/seller_fakes.dart';

Map<String, Object?> _n(String id,
        {String event = 'STOCK_REQUEST_APPROVED',
        String category = 'REQUESTS',
        String body =
            'Your stock request SR-2026-00001 is approved and being prepared.',
        bool read = false,
        bool important = true,
        String? title,
        String? type = 'stock_request',
        String? related = 'r1',
        String? at}) =>
    {
      'id': id,
      'event_key': event,
      'category': category,
      'title': title,
      'body': body,
      'important': important,
      'related_type': type,
      'related_id': related,
      'created_at': at ??
          DateTime.now()
              .toUtc()
              .subtract(const Duration(minutes: 2))
              .toIso8601String(),
      'read_at': read ? '2026-11-17T10:00:00Z' : null,
    };

Map<String, Object?> _page(List<Object?> items,
        {int total = 12,
        int unread = 5,
        int important = 3,
        Map<String, Object?> categories = const {}}) =>
    {
      'items': items,
      'page': 1,
      'size': 20,
      'total': items.length,
      'pages': 1,
      'summary': {
        'total': total,
        'unread': unread,
        'important': important,
        'categories': categories
      }
    };

AppState _state(FakeApi api) =>
    AppState(api: api.client())..selectOperation(operation);
Widget _host(AppState state, Widget home) =>
    AppScope(state: state, child: testApp(home: home));

void main() {
  testWidgets(
      'the center shows titles in the seller\'s language, the counts, and filters on the server',
      (tester) async {
    final api = FakeApi({
      'GET /me/notifications': (_) => (
            200,
            _page([
              _n('n1'),
              _n('n2',
                  event: 'STOCK_LOW',
                  category: 'INVENTORY',
                  body: 'Low stock: Latte',
                  type: 'stock_alert',
                  related: 's1',
                  read: true)
            ])
          ),
      'GET /me/notifications/unread-count': (_) => (200, {'unread': 5}),
    });
    await tester.pumpWidget(_host(_state(api), const NotificationsScreen()));
    await tester.pumpAndSettle();
    expect(find.text('Stock Request Approved'), findsOneWidget);
    expect(find.text('Low Stock Alert'), findsOneWidget);
    expect(find.text('All (12)'), findsOneWidget);
    expect(find.text('Unread (5)'), findsOneWidget);
    expect(find.text('Important (3)'), findsOneWidget);
    expect(find.text('2m'), findsNWidgets(2),
        reason: 'both were created two minutes ago');
    await tester.tap(find.text('Unread (5)'));
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['filter'], 'UNREAD');
  });

  testWidgets(
      'opening a notification marks it read and offers the screen it is about',
      (tester) async {
    final bodies = <Map<String, dynamic>>[];
    final api = FakeApi({
      'GET /me/notifications/n1': (_) => (200, _n('n1')),
      'POST /me/notifications/read': (r) {
        bodies.add(jsonDecode(r.body) as Map<String, dynamic>);
        return (200, {'changed': 1});
      },
      'GET /me/notifications/unread-count': (_) => (200, {'unread': 4}),
    });
    final state = _state(api);
    await tester.pumpWidget(
        _host(state, const NotificationDetailsScreen(notificationId: 'n1')));
    await tester.pumpAndSettle();
    expect(find.text('Stock Request Approved'), findsOneWidget);
    expect(
        find.textContaining('is approved and being prepared'), findsOneWidget);
    expect(find.textContaining('View: Request Details'), findsOneWidget);
    expect(bodies.single['ids'], ['n1']);
    expect(state.unreadNotifications, 4, reason: 'the badge catches up');
  });

  testWidgets('an announcement brings its own title and has no screen to open',
      (tester) async {
    final api = FakeApi({
      'GET /me/notifications/a1': (_) => (
            200,
            _n('a1',
                event: 'ANNOUNCEMENT',
                category: 'SYSTEM',
                title: 'New Product Launch',
                body: 'Please check the product list.',
                type: 'announcement',
                read: true)
          ),
    });
    await tester.pumpWidget(_host(
        _state(api), const NotificationDetailsScreen(notificationId: 'a1')));
    await tester.pumpAndSettle();
    expect(find.text('New Product Launch'), findsOneWidget);
    expect(find.textContaining('View:'), findsNothing);
    expect(api.calls, isNot(contains('POST /me/notifications/read')),
        reason: 'already read');
  });

  testWidgets(
      'long press selects, and the selection can be marked read or cleared together',
      (tester) async {
    final posts = <String, Map<String, dynamic>>{};
    final api = FakeApi({
      'GET /me/notifications': (_) => (200, _page([_n('n1'), _n('n2')])),
      'POST /me/notifications/read': (r) {
        posts['read'] = jsonDecode(r.body) as Map<String, dynamic>;
        return (200, {'changed': 2});
      },
      'POST /me/notifications/clear': (r) {
        posts['clear'] = jsonDecode(r.body) as Map<String, dynamic>;
        return (200, {'changed': 2});
      },
      'GET /me/notifications/unread-count': (_) => (200, {'unread': 0}),
    });
    await tester.pumpWidget(_host(_state(api), const NotificationsScreen()));
    await tester.pumpAndSettle();
    await tester.longPress(find.text('Stock Request Approved').first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Stock Request Approved').last);
    await tester.pumpAndSettle();
    expect(find.text('2 selected'), findsOneWidget);
    await tester.tap(find.text('Mark as read'));
    await tester.pumpAndSettle();
    expect(posts['read']!['ids'], ['n1', 'n2']);

    await tester.longPress(find.text('Stock Request Approved').first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Clear'));
    await tester.pumpAndSettle();
    expect(posts['clear']!['ids'], ['n1']);
  });

  testWidgets(
      'the menu marks everything read or clears what was read, with no ids',
      (tester) async {
    final posts = <String, Map<String, dynamic>>{};
    final api = FakeApi({
      'GET /me/notifications': (_) => (200, _page([_n('n1')])),
      'POST /me/notifications/read': (r) {
        posts['read'] = jsonDecode(r.body) as Map<String, dynamic>;
        return (200, {'changed': 1});
      },
      'POST /me/notifications/clear': (r) {
        posts['clear'] = jsonDecode(r.body) as Map<String, dynamic>;
        return (200, {'changed': 1});
      },
      'GET /me/notifications/unread-count': (_) => (200, {'unread': 0}),
    });
    await tester.pumpWidget(_host(_state(api), const NotificationsScreen()));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(PopupMenuButton<String>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Mark all as read'));
    await tester.pumpAndSettle();
    expect(posts['read']!['ids'], isNull);
    await tester.tap(find.byType(PopupMenuButton<String>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Clear read'));
    await tester.pumpAndSettle();
    expect(posts['clear']!['ids'], isNull);
  });

  testWidgets('categories show how many are new in each', (tester) async {
    final api = FakeApi({
      'GET /me/notifications': (_) => (
            200,
            _page([], categories: {
              'INVENTORY': {'total': 5, 'unread': 3},
              'REQUESTS': {'total': 2, 'unread': 1}
            })
          ),
    });
    await tester
        .pumpWidget(_host(_state(api), const NotificationCategoriesScreen()));
    await tester.pumpAndSettle();
    expect(find.text('Stock & Inventory'), findsOneWidget);
    expect(find.text('3 new'), findsOneWidget);
    expect(find.text('1 new'), findsOneWidget);
    expect(find.text('Operations & Workflow'), findsOneWidget);
    await tester.tap(find.text('Stock & Inventory'));
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['category'], 'INVENTORY');
  });

  testWidgets('preferences switch categories, but money and cash stay on',
      (tester) async {
    final puts = <Map<String, dynamic>>[];
    final api = FakeApi({
      'GET /me/notifications/preferences': (_) => (
            200,
            [
              {
                'category': 'INVENTORY',
                'in_app': true,
                'push': true,
                'locked': false
              },
              {
                'category': 'EARNINGS',
                'in_app': true,
                'push': true,
                'locked': true
              },
            ]
          ),
      'PUT /me/notifications/preferences/INVENTORY': (r) {
        puts.add(jsonDecode(r.body) as Map<String, dynamic>);
        return (200, []);
      },
    });
    await tester
        .pumpWidget(_host(_state(api), const NotificationPreferencesScreen()));
    await tester.pumpAndSettle();
    expect(find.text('Always on'), findsOneWidget);
    expect(find.textContaining('not available yet'), findsOneWidget);
    final locked = tester.widget<SwitchListTile>(
        find.widgetWithText(SwitchListTile, 'Earnings & Payouts'));
    expect(locked.onChanged, isNull);
    await tester.tap(find.widgetWithText(SwitchListTile, 'Stock & Inventory'));
    await tester.pumpAndSettle();
    expect(puts.single['inApp'], false);
  });

  testWidgets('history groups by day and limits to the chosen period',
      (tester) async {
    final now = DateTime.now().toUtc();
    final api = FakeApi({
      'GET /me/notifications': (_) => (
            200,
            _page([
              _n('n1',
                  at: now
                      .subtract(const Duration(minutes: 5))
                      .toIso8601String()),
              _n('n2',
                  at: now
                      .subtract(const Duration(days: 1, minutes: 5))
                      .toIso8601String()),
            ])
          ),
    });
    await tester
        .pumpWidget(_host(_state(api), const NotificationHistoryScreen()));
    await tester.pumpAndSettle();
    expect(api.uris.first.queryParameters['days'], '7');
    expect(find.text('TODAY'), findsOneWidget);
    expect(find.text('YESTERDAY'), findsOneWidget);
    await tester.tap(find.text('Last 30 days'));
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['days'], '30');
  });

  testWidgets('the Home bell shows how many notifications are unread',
      (tester) async {
    final api = FakeApi({});
    final state = _state(api)
      ..user = const UserProfile(
          id: 'u',
          email: 'e',
          fullName: 'Sandra Eto',
          role: 'SELLER',
          operations: [])
      ..unreadNotifications = 5;
    await tester.pumpWidget(_host(state, const Scaffold(body: HomeHeader())));
    await tester.pump();
    expect(find.text('5'), findsOneWidget);
  });
}
