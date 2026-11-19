import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:lutiano_customer/app.dart';
import 'package:lutiano_customer/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

/// A customer with points opens Rewards, redeems the first reward they can afford and sees its QR (against a running local API).
/// Needs the customer 655777001 / Password12345! with at least 400 points. Writes data.
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('log in, open Rewards, redeem, see the QR and the wallet',
      (tester) async {
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

    await tester.tap(find.text('Rewards').last);
    await settle();
    expect(find.textContaining('pts'), findsWidgets);
    expect(find.text('Retry'), findsNothing);

    await tester.tap(find.text('Rewards Catalogue'));
    await settle();
    await tester.tap(find.text('Free Espresso'));
    await settle();
    await tester.tap(find.text('Redeem'));
    await settle();
    expect(find.text('Reward Redeemed!'), findsOneWidget);
    expect(find.text('Show this code to the seller.'), findsOneWidget);
  });
}
