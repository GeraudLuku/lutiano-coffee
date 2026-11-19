import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:lutiano_seller/app.dart';
import 'package:lutiano_seller/state/app_state.dart';

/// Handles the newest customer order against a running local API: accept it, mark it ready, hand it over.
/// Run after a customer placed an order (see the customer app's walkthrough). Writes data.
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('seller accepts, prepares and hands over a customer order',
      (tester) async {
    tester.platformDispatcher.localesTestValue = const [Locale('en', 'US')];
    addTearDown(tester.platformDispatcher.clearLocalesTestValue);
    await tester.pumpWidget(SellerApp(state: AppState()));
    await tester.pumpAndSettle();
    Future<void> settle() => tester.pumpAndSettle(const Duration(seconds: 1),
        EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));

    await tester.enterText(
        find.byType(EditableText).at(0), 'seller@lutiano.local');
    await tester.enterText(find.byType(EditableText).at(1), 'Coffee123!');
    await tester.tap(find.text('Sign In'));
    await settle();

    await tester.scrollUntilVisible(find.text('Customer Orders'), 300,
        scrollable: find.byType(Scrollable).last);
    await tester.tap(find.text('Customer Orders'));
    await settle();
    expect(find.text('Retry'), findsNothing);
    final order = find.textContaining('ORD-').first;
    expect(order, findsOneWidget);
    await tester.tap(order);
    await settle();

    await tester.tap(find.text('Accept Order'));
    await settle();
    await tester.tap(find.text('Mark as Ready'));
    await settle();
    await tester.tap(find.text('Hand Over'));
    await settle();
    await tester.tap(find.text('Confirm Hand Over'));
    await settle();
    expect(find.text('Completed'), findsWidgets);
  });
}
