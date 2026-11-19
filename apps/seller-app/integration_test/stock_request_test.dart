import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:lutiano_seller/app.dart';
import 'package:lutiano_seller/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

/// Sends ONE real stock request as the seeded Mobile Barista (it adds a pending request to the dev database, which a manager
/// can reject or approve in the admin portal). Skipped unless asked for:
///   flutter test integration_test/stock_request_test.dart -d DEVICE --dart-define=LUTIANO_API=http://localhost:8080/api/v1 --dart-define=RUN_WRITE_TESTS=true
const _write = bool.fromEnvironment('RUN_WRITE_TESTS');

Future<void> _settle(WidgetTester tester) => tester.pumpAndSettle(
    const Duration(seconds: 1),
    EnginePhase.sendSemanticsUpdate,
    const Duration(seconds: 30));

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('a mobile barista requests two products and sees the request',
      (tester) async {
    await tester.pumpWidget(SellerApp(state: AppState()));
    await tester.enterText(
        find.byType(EditableText).at(0), 'sandra.eto@lutiano.local');
    await tester.enterText(find.byType(EditableText).at(1), 'Coffee123!');
    await tester.tap(find.text('Sign In'));
    await _settle(tester);

    await tester.tap(find.text('Inventory').last);
    await _settle(tester);
    await tester.tap(find.text('Stock Requests'));
    await _settle(tester);
    expect(find.text('Total requests'), findsOneWidget);
    await tester.tap(find.text('Request Stock'));
    await _settle(tester);

    await tester.tap(find.byType(Checkbox).at(0));
    await tester.tap(find.byType(Checkbox).at(1));
    await tester.pump();
    await tester.tap(find.text('Next'));
    await _settle(tester);
    expect(find.text('Request Summary'), findsOneWidget);
    await tester.tap(find.text('Submit Request'));
    await _settle(tester);

    expect(find.text('Request Details'), findsOneWidget);
    expect(find.textContaining('SR-'), findsWidgets,
        reason: 'the reference comes from the server');
    expect(find.text('Pending'), findsOneWidget);
    expect(find.textContaining('awaiting review'), findsOneWidget);
  }, skip: !_write);
}
