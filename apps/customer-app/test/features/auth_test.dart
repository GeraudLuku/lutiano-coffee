import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_core/testing.dart';
import 'package:material_ui/material_ui.dart';
import 'package:lutiano_customer/features/auth/login_screen.dart';
import 'package:lutiano_customer/features/auth/signup_screen.dart';
import 'package:lutiano_customer/state/app_state.dart';

import '../support/fakes.dart';
import '../support/harness.dart';

void main() {
  testWidgets('logging in with phone and password starts the session',
      (tester) async {
    Object? sent;
    final api = FakeApi({
      'GET /customer/auth/providers': (_) =>
          (200, {'google': false, 'apple': false}),
      'POST /customer/auth/login': (r) {
        sent = r.body;
        return (200, sessionJson());
      },
      'GET /customer/notifications/unread-count': (_) => (200, {'unread': 0}),
      'GET /customer/favourites': (_) => (200, FakeApi.page([])),
    });
    final state = AppState(api: api.client());
    await tester.pumpWidget(testApp(home: const LoginScreen(), state: state));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextFormField).at(0), '677 11 22 33');
    await tester.enterText(find.byType(TextFormField).at(1), 'Customer1234!');
    await tester.tap(find.text('Log In'));
    await tester.pumpAndSettle();

    expect(sent.toString(), contains('+237677112233'));
    expect(state.signedIn, isTrue);
    expect(state.customer!.fullName, 'Ada Lovelace');
    expect(state.api.token, 'tok');
  });

  testWidgets('a wrong phone number is refused before anything is sent',
      (tester) async {
    final api = FakeApi({
      'GET /customer/auth/providers': (_) =>
          (200, {'google': false, 'apple': false})
    });
    await tester.pumpWidget(
        testApp(home: const LoginScreen(), state: AppState(api: api.client())));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField).at(0), '12345');
    await tester.enterText(find.byType(TextFormField).at(1), 'x');
    await tester.tap(find.text('Log In'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Cameroonian mobile number'), findsOneWidget);
    expect(api.calls.where((c) => c.startsWith('POST')), isEmpty);
  });

  testWidgets('sign-up asks for the terms and then sends the code',
      (tester) async {
    tester.view.physicalSize = const Size(800, 1800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final api = FakeApi({
      'POST /customer/auth/signup': (_) =>
          (202, {'status': 'sent', 'resendAfterSeconds': 60})
    });
    await tester.pumpWidget(testApp(
        home: const SignUpScreen(), state: AppState(api: api.client())));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField).at(0), 'Ada Lovelace');
    await tester.enterText(find.byType(TextFormField).at(1), '677112233');
    await tester.tap(find.text('Create Account'));
    await tester.pumpAndSettle();
    expect(find.text('Accept the terms to continue.'), findsOneWidget);
    expect(api.calls, isEmpty);

    await tester.tap(find.byType(Checkbox));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Create Account'));
    await tester.pumpAndSettle();
    expect(api.calls, ['POST /customer/auth/signup']);
    expect(find.text('Verify Your Phone'), findsOneWidget);
  });
}
