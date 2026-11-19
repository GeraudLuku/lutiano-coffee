import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:lutiano_customer/app.dart';
import 'package:lutiano_customer/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

/// A customer opens the inbox, then writes to support (against a running local API). Needs customer 655777001 / Password12345!. Writes data.
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('inbox, then a support request', (tester) async {
    tester.platformDispatcher.localesTestValue = const [Locale('en', 'US')];
    addTearDown(tester.platformDispatcher.clearLocalesTestValue);
    final state = AppState();
    await tester.pumpWidget(CustomerApp(state: state));
    await tester.pumpAndSettle();
    Future<void> settle() => tester.pumpAndSettle(const Duration(seconds: 1),
        EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));

    await tester.tap(find.textContaining('Log In').last);
    await settle();
    await tester.enterText(find.byType(EditableText).at(0), '655777001');
    await tester.enterText(find.byType(EditableText).at(1), 'Password12345!');
    await tester.tap(find.text('Log In').last);
    await settle();
    expect(state.signedIn, isTrue);

    await tester.tap(find.byTooltip('Notifications'));
    await settle();
    expect(find.text('Retry'), findsNothing);
    expect(find.text('Notifications'), findsWidgets);
    await tester.pageBack();
    await settle();

    await tester.tap(find.text('Profile').last);
    await settle();
    await tester.scrollUntilVisible(find.text('Help & Support'), 300,
        scrollable: find.byType(Scrollable).last);
    await tester.tap(find.text('Help & Support'));
    await settle();
    await tester.tap(find.text('Contact Support'));
    await settle();
    await tester.enterText(
        find.byType(EditableText).at(0), 'Question about my points');
    await tester.enterText(
        find.byType(EditableText).at(1), 'How long are my points valid?');
    await tester.tap(find.text('Send Request'));
    await settle();
    expect(find.text('Support Request'), findsOneWidget);
    expect(find.text('The team has not replied yet. You will be notified.'),
        findsOneWidget);
  });
}
