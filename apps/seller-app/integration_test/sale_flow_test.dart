import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:lutiano_seller/app.dart';
import 'package:lutiano_seller/state/app_state.dart';

/// Records ONE real cash sale against a running local API (it changes the dev database: a sale, stock and cash totals).
/// Skipped unless asked for:
///   flutter test integration_test/sale_flow_test.dart -d DEVICE --dart-define=LUTIANO_API=http://localhost:8080/api/v1 --dart-define=RUN_WRITE_TESTS=true
/// The seeded seller must have an open cash shift and stock at their location.
const _write = bool.fromEnvironment('RUN_WRITE_TESTS');

Future<void> _settle(WidgetTester tester) => tester.pumpAndSettle(
    const Duration(seconds: 1),
    EnginePhase.sendSemanticsUpdate,
    const Duration(seconds: 30));

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('a seller records a cash sale', (tester) async {
    await tester.pumpWidget(SellerApp(state: AppState()));
    await tester.enterText(
        find.byType(EditableText).at(0), 'seller@lutiano.local');
    await tester.enterText(find.byType(EditableText).at(1), 'Coffee123!');
    await tester.tap(find.text('Sign In'));
    await _settle(tester);

    await tester.tap(find.text('New Sale'));
    await _settle(tester);
    await tester.tap(find.byTooltip('Add to cart').first);
    await tester.pump();
    await tester.tap(find.text('View Cart'));
    await _settle(tester);
    await tester.tap(find.text('Proceed to Payment'));
    await _settle(tester);
    await tester.tap(find.text('Confirm Payment'));
    await _settle(tester);

    expect(find.text('Sale Completed!'), findsOneWidget);
    expect(find.textContaining('SL-'), findsWidgets,
        reason: 'the confirmation shows the sale reference from the server');
  }, skip: !_write);
}
