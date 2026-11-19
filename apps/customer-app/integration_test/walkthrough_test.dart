import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:lutiano_customer/app.dart';
import 'package:lutiano_customer/features/menu/menu_widgets.dart';
import 'package:lutiano_customer/state/app_state.dart';

/// A new customer signs up (dev code 123456), browses the menu and puts a product in the cart, against a running local API.
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('sign up, browse the menu, add to the cart', (tester) async {
    tester.platformDispatcher.localesTestValue = const [Locale('en', 'US')];
    addTearDown(tester.platformDispatcher.clearLocalesTestValue);
    final state = AppState();
    await tester.pumpWidget(CustomerApp(state: state));
    await tester.pumpAndSettle();

    Future<void> settle() => tester.pumpAndSettle(const Duration(seconds: 1),
        EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));

    expect(find.text('Get Started'), findsOneWidget);
    await tester.tap(find.text('Get Started'));
    await settle();

    final phone =
        '6${(DateTime.now().millisecondsSinceEpoch % 100000000).toString().padLeft(8, '0')}';
    await tester.enterText(find.byType(EditableText).at(0), 'Test Customer');
    await tester.enterText(find.byType(EditableText).at(1), phone);
    await tester.ensureVisible(find.byType(Checkbox));
    await tester.tap(find.byType(Checkbox));
    await tester.pump();
    await tester.ensureVisible(find.text('Create Account'));
    await tester.tap(find.text('Create Account'));
    await settle();

    expect(find.text('Verify Your Phone'), findsOneWidget);
    await tester.enterText(find.byType(EditableText).first, '123456');
    await tester.pump();
    await tester.tap(find.text('Verify Account'));
    await settle();

    expect(state.signedIn, isTrue);
    expect(find.textContaining('Hello, Test'), findsOneWidget);

    await tester.tap(find.text('Menu').last);
    await settle();
    expect(find.text('Retry'), findsNothing);
    await tester.tap(find.byType(MenuRow).first);
    await settle();
    expect(find.textContaining('Add to Cart'), findsOneWidget);

    // no sales point yet: add to cart asks for one
    await tester.tap(find.textContaining('Add to Cart'));
    await settle();
    expect(find.text('Sales Points'), findsOneWidget);
    await tester.tap(find.byType(AppCard).first);
    await settle();
    await tester.tap(find.text('Select this sales point'));
    await settle();
    expect(find.text('Added to cart!'), findsOneWidget);
    await tester.tap(find.textContaining('View cart'));
    await settle();

    // checkout in cash and place the order
    await tester.tap(find.text('Proceed to Checkout'));
    await settle();
    await tester.tap(find.text('Place Order'));
    await settle();
    expect(find.text('Order Placed!'), findsOneWidget);
    await tester.tap(find.text('View Order Details'));
    await settle();
    expect(find.text('Order Details'), findsOneWidget);
    expect(find.text('Order placed'), findsWidgets);
  });
}
