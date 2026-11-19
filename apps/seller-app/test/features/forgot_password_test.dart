import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:lutiano_seller/features/auth/forgot_password_screen.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';

Widget _host(AppState state) => AppScope(
    state: state,
    child: testApp(
        home: const ForgotPasswordScreen(initialEmail: 'brice@lutiano.local')));

void main() {
  testWidgets('asks for a code, then sets a new password', (tester) async {
    Map<String, dynamic>? reset;
    final api = FakeApi({
      'POST /auth/password/forgot': (_) => (200, {'status': 'sent'}),
      'POST /auth/password/reset': (r) {
        reset = {'code': r.body};
        return (200, {'status': 'reset'});
      },
    });
    await tester.pumpWidget(_host(AppState(api: api.client())));
    await tester.tap(find.text('Send Code'));
    await tester.pumpAndSettle();
    expect(find.text('Set New Password'), findsOneWidget);
    await tester.enterText(
        find.widgetWithText(TextField, 'Code from the SMS'), '123456');
    await tester.enterText(
        find.widgetWithText(TextField, 'New password (10+ characters)'),
        'NewCoffee456!');
    await tester.tap(find.text('Set New Password'));
    await tester.pumpAndSettle();
    expect(
        api.calls, ['POST /auth/password/forgot', 'POST /auth/password/reset']);
    expect(reset!['code'], contains('"code":"123456"'));
  });

  testWidgets('a wrong code keeps the form and shows the message',
      (tester) async {
    final api = FakeApi({
      'POST /auth/password/forgot': (_) => (200, {'status': 'sent'}),
      'POST /auth/password/reset': (_) => (
            400,
            {
              'error': {'message': 'That code is not correct. 4 tries left.'}
            }
          ),
    });
    await tester.pumpWidget(_host(AppState(api: api.client())));
    await tester.tap(find.text('Send Code'));
    await tester.pumpAndSettle();
    await tester.enterText(
        find.widgetWithText(TextField, 'Code from the SMS'), '000000');
    await tester.enterText(
        find.widgetWithText(TextField, 'New password (10+ characters)'),
        'NewCoffee456!');
    await tester.tap(find.text('Set New Password'));
    await tester.pumpAndSettle();
    expect(
        find.text('That code is not correct. 4 tries left.'), findsOneWidget);
  });

  testWidgets('a short password is refused before anything is sent',
      (tester) async {
    final api = FakeApi({
      'POST /auth/password/forgot': (_) => (200, {'status': 'sent'})
    });
    await tester.pumpWidget(_host(AppState(api: api.client())));
    await tester.tap(find.text('Send Code'));
    await tester.pumpAndSettle();
    await tester.enterText(
        find.widgetWithText(TextField, 'Code from the SMS'), '123456');
    await tester.enterText(
        find.widgetWithText(TextField, 'New password (10+ characters)'),
        'short');
    await tester.tap(find.text('Set New Password'));
    await tester.pumpAndSettle();
    expect(find.textContaining('at least 10 characters'), findsOneWidget);
    expect(api.calls, ['POST /auth/password/forgot']);
  });
}
