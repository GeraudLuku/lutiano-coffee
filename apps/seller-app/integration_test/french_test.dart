import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:lutiano_seller/app.dart';
import 'package:lutiano_seller/state/app_state.dart';

/// The app in French (the default language), against a running local API.
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('sign-in, Home and the tabs are in French', (tester) async {
    tester.platformDispatcher.localesTestValue = const [Locale('fr', 'FR')];
    addTearDown(tester.platformDispatcher.clearLocalesTestValue);
    await tester.pumpWidget(SellerApp(state: AppState()));
    expect(find.text('Se connecter'), findsOneWidget);
    expect(find.text('Mot de passe oublié ?'), findsOneWidget);

    await tester.enterText(
        find.byType(EditableText).at(0), 'seller@lutiano.local');
    await tester.enterText(find.byType(EditableText).at(1), 'Coffee123!');
    await tester.tap(find.text('Se connecter'));
    await tester.pumpAndSettle(const Duration(seconds: 1),
        EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));

    expect(find.text('Nouvelle vente'), findsOneWidget);
    expect(find.text('Ma journée'), findsOneWidget);
    expect(find.text('ACTIONS RAPIDES'), findsOneWidget);
    for (final tab in ['Ventes', 'Inventaire', 'Gains', 'Profil', 'Accueil']) {
      await tester.tap(find.text(tab).last);
      await tester.pumpAndSettle(const Duration(seconds: 1),
          EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));
      expect(find.text('Réessayer'), findsNothing,
          reason: '$tab failed to load');
    }
    // the long French labels of the targets screens fit (an overflow would fail the test)
    await tester.tap(find.text('Gains').last);
    await tester.pumpAndSettle(const Duration(seconds: 1),
        EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));
    await tester.scrollUntilVisible(find.text('Objectifs et primes'), 300,
        scrollable: find.byType(Scrollable).last);
    await tester.tap(find.text('Objectifs et primes'));
    await tester.pumpAndSettle(const Duration(seconds: 1),
        EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));
    expect(find.text('Réessayer'), findsNothing);
    await tester.tap(find.text('Voir l\'objectif'));
    await tester.pumpAndSettle(const Duration(seconds: 1),
        EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));
    expect(find.text('Détails de l\'objectif'), findsOneWidget);
  });
}
