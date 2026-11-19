import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/data/models.dart';
import 'package:lutiano_seller/features/profile/app_settings_screen.dart';
import 'package:lutiano_seller/features/profile/edit_profile_screen.dart';
import 'package:lutiano_seller/features/profile/faq_screen.dart';
import 'package:lutiano_seller/features/profile/login_activity_screen.dart';
import 'package:lutiano_seller/features/profile/my_support_requests_screen.dart';
import 'package:lutiano_seller/features/profile/security_screen.dart';
import 'package:lutiano_seller/features/profile/support_details_screen.dart';
import 'package:lutiano_seller/features/profile/support_request_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_settings.dart';
import 'package:lutiano_seller/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';
import '../support/seller_fakes.dart';

Map<String, Object?> _user(
        {String name = 'Sandra Eto', String? phone = '+237 673 44 55 66'}) =>
    {
      'id': 'u1',
      'email': 's@x.local',
      'full_name': name,
      'role': 'SELLER',
      'seller_role': 'MOBILE_BARISTA',
      'phone': phone,
      'operations': [operationJson],
    };

Map<String, Object?> _page(List<Object?> items) => {
      'items': items,
      'page': 1,
      'size': 20,
      'total': items.length,
      'pages': 1,
      'summary': {}
    };

Map<String, Object?> _support({String? reply, String status = 'OPEN'}) => {
      'id': 'q1',
      'reference': 'SP-2026-00001',
      'type': 'ISSUE',
      'subject': 'Cannot record a sale',
      'description': 'It fails at payment',
      'status': status,
      'created_at': '2026-11-17T10:00:00Z',
      'reply': reply,
    };

AppState _state(FakeApi api, {AppSettings? settings}) =>
    AppState(api: api.client(), settings: settings)
      ..user = UserProfile.fromJson(_user())
      ..operations = [operation]
      ..selectOperation(operation);

Widget _host(AppState state, Widget home) =>
    AppScope(state: state, child: testApp(home: home));

void main() {
  testWidgets('editing the profile sends the new name', (tester) async {
    Map<String, dynamic>? sent;
    final api = FakeApi({
      'PUT /me/profile': (r) {
        sent = jsonDecode(r.body) as Map<String, dynamic>;
        return (200, _user(name: 'Sandra E. Eto'));
      },
    });
    final state = _state(api);
    await tester.pumpWidget(_host(state, const EditProfileScreen()));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).first, 'Sandra E. Eto');
    await tester.tap(find.text('Save changes'));
    await tester.pumpAndSettle();
    expect(sent!['fullName'], 'Sandra E. Eto');
    expect(state.user!.fullName, 'Sandra E. Eto');
  });

  testWidgets('a new phone number is confirmed with the code sent to it',
      (tester) async {
    final calls = <String>[];
    final api = FakeApi({
      'POST /me/phone': (r) {
        calls.add('start ${jsonDecode(r.body)['phone']}');
        return (200, {'status': 'sent'});
      },
      'POST /me/phone/confirm': (r) {
        calls.add('confirm ${jsonDecode(r.body)['code']}');
        return (200, _user(phone: '+237690000009'));
      },
    });
    final state = _state(api);
    await tester.pumpWidget(_host(state, const EditProfileScreen()));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Phone'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'New phone number'),
        '+237 690 00 00 09');
    await tester.tap(find.text('Send code'));
    await tester.pumpAndSettle();
    expect(find.textContaining('We sent a code to'), findsOneWidget);
    await tester.enterText(
        find.widgetWithText(TextField, 'Confirmation code'), '123456');
    await tester.tap(find.text('Confirm'));
    await tester.pumpAndSettle();
    expect(calls, ['start +237 690 00 00 09', 'confirm 123456']);
    expect(state.user!.phone, '+237690000009');
  });

  testWidgets(
      'a refused phone number shows the server\'s message and stays in the dialog',
      (tester) async {
    final api = FakeApi({
      'POST /me/phone': (_) => (
            409,
            {
              'error': {'message': 'Another account already uses this number.'}
            }
          ),
    });
    await tester.pumpWidget(_host(_state(api), const EditProfileScreen()));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Phone'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'New phone number'),
        '+237 690 00 00 01');
    await tester.tap(find.text('Send code'));
    await tester.pumpAndSettle();
    expect(
        find.text('Another account already uses this number.'), findsOneWidget);
  });

  testWidgets(
      'signing out of other devices keeps this phone signed in with the new token',
      (tester) async {
    final api = FakeApi({
      'POST /me/sessions/revoke': (_) =>
          (200, {'token': 'fresh-token', 'expiresAt': '2026-11-19T00:00:00Z'}),
    });
    final state = _state(api);
    state.api.token = 'old-token';
    await tester.pumpWidget(_host(state, const SecurityScreen()));
    await tester.pumpAndSettle();
    expect(find.text('Required for administrators only'), findsOneWidget);
    await tester.tap(find.text('Sign out of all other devices'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Confirm'));
    await tester.pumpAndSettle();
    expect(state.api.token, 'fresh-token');
  });

  testWidgets('login activity lists sign-ins with their device',
      (tester) async {
    final api = FakeApi({
      'GET /me/activity': (_) => (
            200,
            _page([
              {
                'action': 'SIGNED_IN',
                'client': 'Seller app',
                'created_at': '2026-11-17T10:00:00Z'
              },
              {
                'action': 'PASSWORD_CHANGED',
                'client': null,
                'created_at': '2026-11-16T10:00:00Z'
              },
            ])
          ),
    });
    await tester.pumpWidget(_host(_state(api), const LoginActivityScreen()));
    await tester.pumpAndSettle();
    expect(find.text('Signed in'), findsOneWidget);
    expect(find.text('Seller app'), findsOneWidget);
    expect(find.text('Password changed'), findsOneWidget);
  });

  testWidgets(
      'settings: the language changes the app at once, the receipt switch is remembered',
      (tester) async {
    final api = FakeApi({'PUT /me/profile': (_) => (200, _user())});
    final settings = AppSettings();
    final state = _state(api, settings: settings);
    await tester.pumpWidget(_host(state, const AppSettingsScreen()));
    await tester.pumpAndSettle();
    expect(find.text('Follow my phone'), findsOneWidget);
    await tester.tap(find.text('Language'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Français'));
    await tester.pumpAndSettle();
    expect(settings.languageCode, 'fr');
    expect(api.calls, contains('PUT /me/profile'),
        reason: 'the language of the SMS follows');
    expect(settings.showReceipt, true);
    await tester.tap(find.byType(SwitchListTile));
    await tester.pumpAndSettle();
    expect(settings.showReceipt, false);
  });

  testWidgets(
      'a support request is sent as a form with the type, subject and description',
      (tester) async {
    String? body;
    final api = FakeApi({
      'POST /me/support': (r) {
        body = r.body;
        return (201, _support());
      },
    });
    await tester.pumpWidget(_host(_state(api), const SupportRequestScreen()));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Submit Request'));
    await tester.pumpAndSettle();
    expect(
        find.text('Fill in the subject and the description.'), findsOneWidget);
    expect(api.calls, isEmpty);
    await tester.enterText(
        find.widgetWithText(TextField, 'Subject'), 'Cannot record a sale');
    await tester.enterText(
        find.widgetWithText(TextField, 'Description'), 'It fails at payment');
    await tester.tap(find.text('Submit Request'));
    await tester.pumpAndSettle();
    expect(body, contains('Cannot record a sale'));
    expect(body, contains('QUESTION'));
    expect(api.calls, ['POST /me/support']);
  });

  testWidgets(
      'support requests show their status, and the team\'s reply when there is one',
      (tester) async {
    final api = FakeApi({
      'GET /me/support': (_) => (
            200,
            _page([
              _support(),
              {
                ..._support(),
                'id': 'q2',
                'subject': 'Another',
                'status': 'RESOLVED'
              }
            ])
          ),
      'GET /me/support/q1': (_) => (
            200,
            _support(reply: 'Fixed in the next update.', status: 'RESOLVED')
          ),
    });
    await tester
        .pumpWidget(_host(_state(api), const MySupportRequestsScreen()));
    await tester.pumpAndSettle();
    expect(find.text('Open'), findsOneWidget);
    expect(find.text('Resolved'), findsOneWidget);
    await tester.tap(find.text('Cannot record a sale'));
    await tester.pumpAndSettle();
    expect(find.text('Fixed in the next update.'), findsOneWidget);
  });

  testWidgets('a request without a reply says so', (tester) async {
    final api = FakeApi({'GET /me/support/q1': (_) => (200, _support())});
    await tester.pumpWidget(
        _host(_state(api), const SupportDetailsScreen(requestId: 'q1')));
    await tester.pumpAndSettle();
    expect(find.textContaining('No reply yet'), findsOneWidget);
  });

  testWidgets('the FAQ answers open on tap', (tester) async {
    await tester.pumpWidget(_host(_state(FakeApi({})), const FaqScreen()));
    await tester.pumpAndSettle();
    await tester.tap(find.text('How do I receive stock?'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Count what arrived'), findsOneWidget);
  });
}
